from django.shortcuts import render, redirect
from django.http import HttpResponse, JsonResponse
from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.response import Response

from .models import Image  # 确保导入的是修改后的 Image 模型
from datetime import datetime
import os

# Create your views here.

def testurl(request):
    return HttpResponse("<h1>testurl you are looking for</h1>")

def yesredirect(request, year):
    return HttpResponse(f"{year} you are looking for")

def image(request):
    return render(request, "images.html")  # 修改模板文件名以匹配图片上传

def info(request):
    image_name = request.POST['name']
    time_code = datetime.now().strftime("%Y%m%d%H%M")  # 生成 YYYYMMDDHHMM 格式时间代码
    image_file = request.FILES.get('image_file')  # 获取上传的图片文件
    task_name = request.POST.get('task_name', '')  # 获取任务名称
    executor = request.POST.get('executor', '')  # 获取执行人
    place = request.POST.get('place', '')  # 获取位置信息

    Image.objects.create(
        name=image_name,
        time_code=time_code,
        image_file=image_file,
        task_name=task_name,
        executor=executor,
        place=place
    )

    print(image_name, time_code, task_name, executor)
    return render(request, "over_image.html")  # 修改模板文件名以匹配图片上传完成

def image_info(request):
    if request.method == 'POST':
        # 调试：打印所有接收到的参数
        print("Received POST params:", request.POST)
        print("Received FILES:", request.FILES)
        
        # 从FILES中获取文本参数，因为Android客户端将所有参数都作为文件部分发送
        if 'name' in request.FILES:
            image_name = request.FILES['name'].read().decode('utf-8')
        else:
            image_name = request.POST.get('name', 'default_name')
            
        # 获取图片文件
        image_file = request.FILES.get('image_file')
        
        # 获取其他参数，优先从FILES中读取
        place = ''
        if 'place' in request.FILES:
            place = request.FILES['place'].read().decode('utf-8')
        else:
            place = request.POST.get('place', '')
            
        task_name = ''
        if 'task_name' in request.FILES:
            task_name = request.FILES['task_name'].read().decode('utf-8')
        else:
            task_name = request.POST.get('task_name', '')
            
        executor = ''
        if 'executor' in request.FILES:
            executor = request.FILES['executor'].read().decode('utf-8')
        else:
            executor = request.POST.get('executor', '')

        print(f"提取的值 - 名称: {image_name}, 位置: {place}, 任务名称: {task_name}, 执行人: {executor}")

        if not image_file:
            return JsonResponse({"error": "没有上传图片文件"}, status=400)

        image_obj = Image.objects.create(
            name=image_name,
            time_code=datetime.now().strftime("%Y%m%d%H%M"),
            image_file=image_file,
            place=place,  # 保存位置值
            task_name=task_name,  # 保存任务名称
            executor=executor  # 保存执行人
        )

        return JsonResponse({
            "message": "上传成功",
            "image_id": image_obj.id,
            "image_name": image_obj.name,
            "image_file": image_obj.image_file.url,
            "place": image_obj.place,
            "task_name": image_obj.task_name,
            "executor": image_obj.executor
        })

    return JsonResponse({"error": "无效的请求方法"}, status=400)

def image_list_view(request):
    images = Image.objects.all()

    # 提取文件名部分
    for image in images:
        image.filename = os.path.basename(image.image_file.name)

    context = {'images': images}
    return render(request, "list_image.html", context)  # 修改模板文件名以匹配图片列表

def image_list_page(request):
    if request.method == 'GET':
        images = Image.objects.all().values('id', 'name', 'time_code', 'image_file', 'place', 'task_name', 'executor')

        # 提取文件名
        image_list = []
        for image in images:
            filename = os.path.basename(image["image_file"])
            image_list.append({
                "id": image["id"],
                "name": image["name"],
                "time_code": image["time_code"],
                "filename": filename,
                "image_file": image["image_file"],
                "place": image.get("place", ""),
                "task_name": image.get("task_name", ""),
                "executor": image.get("executor", "")
            })

        return JsonResponse({"images": image_list}, safe=False)

    return JsonResponse({"error": "Invalid request method"}, status=400)

# 返回 HTML 页面

@api_view(['GET'])
def image_list(request):
    images = Image.objects.all().values('id', 'name', 'time_code', 'image_file', 'place', 'task_name', 'executor')
    return Response(list(images))

@api_view(['DELETE'])
def delete_image(request, image_id):
    try:
        image = Image.objects.get(id=image_id)

        # 删除图片文件
        if image.image_file:
            image.image_file.delete()

        image.delete()
        return Response({"message": "Image deleted successfully"}, status=status.HTTP_204_NO_CONTENT)

    except Image.DoesNotExist:
        return Response({"error": "Image not found"}, status=status.HTTP_404_NOT_FOUND)

def delete_image2(request):
    # 获取要删除的记录的 ID
    id = request.POST['id']

    try:
        # 获取要删除的 Image 对象
        image = Image.objects.get(id=id)

        # 删除关联的图片文件
        if image.image_file:
            image.image_file.delete()  # 删除图片文件

        # 删除 Image 记录
        image.delete()

        return redirect('/image/list_image')  # 重定向到图片列表页面
    except Image.DoesNotExist:
        # 如果 Image 对象不存在，返回错误信息或页面
        return HttpResponse("Image not found", status=404)

def image_detail(request, image_id):
    """
    图片详情页面，显示图片的所有参数和预览
    """
    try:
        # 获取图片信息
        image = Image.objects.get(id=image_id)
        
        # 提取文件名
        image.filename = os.path.basename(image.image_file.name)
        
        context = {
            'image': image
        }
        
        return render(request, 'image_detail.html', context)
        
    except Image.DoesNotExist:
        return HttpResponse("图片不存在", status=404)