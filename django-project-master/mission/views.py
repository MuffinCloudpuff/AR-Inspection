from django.shortcuts import render, redirect
from django.http import HttpResponse
from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.response import Response

from mission.models import Mission

from django.http import JsonResponse

# Create your views here.
def testurl(request):
    return HttpResponse("<h1>testurl you are looking for</h1>")
def yesredirect(request,year):
    return HttpResponse(f"{year} you are looking for")

def mission(request):
    return render(request,"missions.html")

def info(request):
    mission_task = request.POST['task']
    mission_name = request.POST['name']
    mission_state = request.POST['state']

    Mission.objects.create(task=mission_task, name=mission_name, state=mission_state)
    print(mission_task,mission_name,mission_state)
    return render(request,"over_mission.html")

def mission_info(request):
    if request.method == 'POST':
        mission_task = request.POST.get('task')
        mission_name = request.POST.get('name')
        mission_state = request.POST.get('state')

        mission_obj = Mission.objects.create(
            task=mission_task,
            name=mission_name,
            state=mission_state
        )
        return JsonResponse({
            "message": "Upload successful",
            "mission_id": mission_obj.id,
            "mission_task": mission_obj.task,
            "mission_name": mission_obj.name,
            "mission_state": mission_obj.state
        })
    return JsonResponse({"error": "Invalid request"}, status=400)
def mission_list_view(request):
    missions = Mission.objects.all()
    context = {'missions': missions}
    return render(request,"list_mission.html",context)

def mission_list_page(request):
    if request.method == 'GET':
        missions = Mission.objects.all().values('id', 'task', 'name', 'state')

        mission_list = []
        for mission in missions:
            mission_list.append({
                "id": mission["id"],
                "task": mission["task"],
                "name": mission["name"],
                "state": mission["state"]
            })

        return JsonResponse({"missions": mission_list}, safe=False)

    return JsonResponse({"error": "Invalid request method"}, status=400)

@api_view(['GET'])
def mission_list(request):
    students = Mission.objects.all().values('id', 'task', 'name', 'state')
    return Response(list(students))

@api_view(['DELETE'])
def delete_mission(request, mission_id):
    try:
        mission = Mission.objects.get(id=mission_id)
        mission.delete()
        return Response({"message": "Mission deleted successfully"}, status=status.HTTP_204_NO_CONTENT)
    except Mission.DoesNotExist:
        return Response({"error": "Mission not found"}, status=status.HTTP_404_NOT_FOUND)

def delete_mission2(request):
    id = request.POST['id']
    mission = Mission.objects.get(id=id)
    mission.delete()
    return redirect('/mission/list_mission')


def mission_by_name(request, name):
    """
    根据 name 参数查询任务，并返回 JSON 格式的数据
    """
    missions = Mission.objects.filter(name=name).values('id', 'task', 'name', 'state')
    return JsonResponse({"missions": list(missions)}, safe=False)

@api_view(['POST'])
def complete_mission(request, mission_id):
    """
    将指定ID的任务状态修改为已完成
    """
    try:
        mission = Mission.objects.get(id=mission_id)
        # 检查当前状态
        previous_state = mission.state
        
        # 更新任务状态为已完成
        mission.state = "已完成"
        mission.save()
        
        # 如果任务状态从"未完成"变为"已完成"，则自动生成报告
        if previous_state != "已完成":
            # 获取生成的报告数据
            from photo.models import Image
            
            # 获取所有有位置信息的照片
            related_photos = Image.objects.filter(
                place__isnull=False
            ).exclude(place="").order_by('place', '-time_code')
            
            # 根据task_name字段筛选照片，只要任务名称一致即可
            filtered_photos = []
            for photo in related_photos:
                # 优先检查task_name字段
                if photo.task_name and photo.task_name == mission.task:
                    filtered_photos.append(photo)
                # 兼容旧数据：如果未设置task_name但name与执行人一致，也包含
                elif not photo.task_name and photo.name == mission.name:
                    filtered_photos.append(photo)
            
            # 按位置分组整理照片
            photo_groups = {}
            for photo in filtered_photos:
                if photo.place not in photo_groups:
                    photo_groups[photo.place] = []
                photo_groups[photo.place].append({
                    "id": photo.id,
                    "name": photo.name,
                    "time_code": photo.time_code,
                    "image_url": photo.image_file.url if photo.image_file else "",
                    "place": photo.place,
                    "task_name": photo.task_name,
                    "executor": photo.executor
                })
            
            # 构建报告数据
            report = {
                "mission_id": mission.id,
                "task_name": mission.task,
                "executor": mission.name,
                "state": mission.state,
                "photo_groups": photo_groups
            }
            
            return Response({
                "message": "任务已完成，并已生成报告",
                "report": report
            }, status=status.HTTP_200_OK)
        
        return Response({"message": "任务已成功完成"}, status=status.HTTP_200_OK)
    except Mission.DoesNotExist:
        return Response({"error": "任务不存在"}, status=status.HTTP_404_NOT_FOUND)

@api_view(['GET'])
def unfinished_missions(request):
    """
    获取所有未完成的任务
    """
    missions = Mission.objects.filter(state="未完成").values('id', 'task', 'name', 'state')
    return Response(list(missions))

def mission_detail(request, mission_id):
    """
    任务详情页面，展示任务信息和相关图片
    """
    from photo.models import Image
    
    # 获取任务信息
    try:
        mission = Mission.objects.get(id=mission_id)
    except Mission.DoesNotExist:
        return HttpResponse("任务不存在", status=404)
    
    # 获取所有照片，按时间倒序排列
    all_photos = Image.objects.all().order_by('-time_code')
    
    # 筛选与当前任务相关的照片，只要任务名称一致即可
    photos = []
    for photo in all_photos:
        # 优先检查task_name字段
        if photo.task_name and photo.task_name == mission.task:
            photos.append(photo)
        # 兼容旧数据：如果未设置task_name但name与执行人一致，也包含
        elif not photo.task_name and photo.name == mission.name:
            photos.append(photo)
    
    context = {
        'mission': mission,
        'photos': photos
    }
    
    return render(request, 'mission_detail.html', context)

@api_view(['POST'])
def generate_mission_report(request, mission_id):
    """
    当任务状态从未完成变为已完成时，生成任务报告
    将任务名称和执行人相同的图片整合到一起，按位置分组显示
    """
    from photo.models import Image
    
    try:
        mission = Mission.objects.get(id=mission_id)
        
        # 检查任务状态是否为"已完成"
        if mission.state != "已完成":
            return Response({
                "error": "只能为已完成的任务生成报告"
            }, status=status.HTTP_400_BAD_REQUEST)
        
        # 获取所有有位置信息的照片
        related_photos = Image.objects.filter(
            place__isnull=False
        ).exclude(place="").order_by('place', '-time_code')
        
        # 根据task_name字段筛选照片，只要任务名称一致即可
        filtered_photos = []
        for photo in related_photos:
            # 优先检查task_name字段
            if photo.task_name and photo.task_name == mission.task:
                filtered_photos.append(photo)
            # 兼容旧数据：如果未设置task_name但name与执行人一致，也包含
            elif not photo.task_name and photo.name == mission.name:
                filtered_photos.append(photo)
        
        # 按位置分组整理照片
        photo_groups = {}
        for photo in filtered_photos:
            if photo.place not in photo_groups:
                photo_groups[photo.place] = []
            photo_groups[photo.place].append({
                "id": photo.id,
                "name": photo.name,
                "time_code": photo.time_code,
                "image_url": photo.image_file.url if photo.image_file else "",
                "place": photo.place,
                "task_name": photo.task_name,
                "executor": photo.executor
            })
        
        # 构建报告数据
        report = {
            "mission_id": mission.id,
            "task_name": mission.task,
            "executor": mission.name,
            "state": mission.state,
            "photo_groups": photo_groups
        }
        
        return Response(report, status=status.HTTP_200_OK)
        
    except Mission.DoesNotExist:
        return Response({"error": "任务不存在"}, status=status.HTTP_404_NOT_FOUND)

def mission_report(request, mission_id):
    """
    任务报告页面，按照位置参数分组显示任务的相关照片
    """
    from photo.models import Image
    
    try:
        # 获取任务信息
        mission = Mission.objects.get(id=mission_id)
        
        # 检查任务状态是否为"已完成"
        if mission.state != "已完成":
            return HttpResponse("只能为已完成的任务生成报告", status=400)
        
        # 获取所有有位置信息的照片
        related_photos = Image.objects.filter(
            place__isnull=False
        ).exclude(place="").order_by('place', '-time_code')
        
        # 根据task_name字段筛选照片，只要任务名称一致即可
        filtered_photos = []
        for photo in related_photos:
            # 优先检查task_name字段
            if photo.task_name and photo.task_name == mission.task:
                filtered_photos.append(photo)
            # 兼容旧数据：如果未设置task_name但name与执行人一致，也包含
            elif not photo.task_name and photo.name == mission.name:
                filtered_photos.append(photo)
        
        # 按位置分组整理照片
        photo_groups = {}
        for photo in filtered_photos:
            if photo.place not in photo_groups:
                photo_groups[photo.place] = []
            photo_groups[photo.place].append({
                "id": photo.id,
                "name": photo.name,
                "time_code": photo.time_code,
                "image_url": photo.image_file.url if photo.image_file else "",
                "place": photo.place,
                "task_name": photo.task_name,
                "executor": photo.executor
            })
        
        context = {
            'mission': mission,
            'photo_groups': photo_groups
        }
        
        return render(request, 'mission_report.html', context)
        
    except Mission.DoesNotExist:
        return HttpResponse("任务不存在", status=404)

