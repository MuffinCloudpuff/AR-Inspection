import os

from django.shortcuts import render, redirect
from django.http import HttpResponse
from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.response import Response

from audiorecord.models import Audio
from datetime import datetime

from django.http import JsonResponse
# Create your views here.

def testurl(request):
    return HttpResponse("<h1>testurl you are looking for</h1>")
def yesredirect(request,year):
    return HttpResponse(f"{year} you are looking for")

def audio(request):
    return render(request,"audios.html")

def info(request):
    audio_name = request.POST['name']
    time_code = datetime.now().strftime("%Y%m%d%H%M")  # 生成 YYYYMMDDHHMM 格式时间代码
    audio_file = request.FILES.get('audio_file')  # 获取上传的语音文件

    Audio.objects.create(
        name=audio_name,
        time_code=time_code,
        audio_file=audio_file
    )

    print(audio_name, time_code)
    return render(request, "over_audio.html")

def audio_info(request):
    if request.method == 'POST':
        audio_name = request.POST.get('name', 'default_name')
        audio_file = request.FILES.get('audio_file')

        if not audio_file:
            return JsonResponse({"error": "No audio file uploaded"}, status=400)

        audio_obj = Audio.objects.create(
            name=audio_name,
            time_code=datetime.now().strftime("%Y%m%d%H%M"),
            audio_file=audio_file
        )

        return JsonResponse({
            "message": "Upload successful",
            "audio_id": audio_obj.id,
            "audio_name": audio_obj.name,
            "audio_file": audio_obj.audio_file.url
        })

    return JsonResponse({"error": "Invalid request"}, status=400)

def audio_list_view(request):
    audios = Audio.objects.all()

    # 提取文件名部分
    for audio in audios:
        audio.filename = os.path.basename(audio.audio_file.name)

    context = {'audios': audios}
    return render(request, "list_audio.html", context)

def audio_list_page(request):
    if request.method == 'GET':
        audios = Audio.objects.all().values('id', 'name', 'time_code', 'audio_file')

        # 提取文件名
        audio_list = []
        for audio in audios:
            filename = os.path.basename(audio["audio_file"])
            audio_list.append({
                "id": audio["id"],
                "name": audio["name"],
                "time_code": audio["time_code"],
                "filename": filename,
                "audio_file": audio["audio_file"]
            })

        return JsonResponse({"audios": audio_list}, safe=False)

    return JsonResponse({"error": "Invalid request method"}, status=400)

# 返回 HTML 页面


@api_view(['GET'])
def audio_list(request):
    audios = Audio.objects.all().values('id', 'name', 'time_code', 'audio_file')
    return Response(list(audios))

# @api_view(['DELETE'])
# def delete_audio(request, audio_id):
#     try:
#         audio = Audio.objects.get(id=audio_id)
#         audio.delete()
#         return Response(status=status.HTTP_204_NO_CONTENT)
#     except Audio.DoesNotExist:
#         return Response(status=status.HTTP_404_NOT_FOUND)

@api_view(['DELETE'])
def delete_audio(request, audio_id):
    try:
        audio = Audio.objects.get(id=audio_id)

        # 删除音频文件
        if audio.audio_file:
            audio.audio_file.delete()

        audio.delete()
        return Response({"message": "Audio deleted successfully"}, status=status.HTTP_204_NO_CONTENT)

    except Audio.DoesNotExist:
        return Response({"error": "Audio not found"}, status=status.HTTP_404_NOT_FOUND)

def delete_audio2(request):
    # 获取要删除的记录的 ID
    id = request.POST['id']

    try:
        # 获取要删除的 Audio 对象
        audio = Audio.objects.get(id=id)

        # 删除关联的音频文件
        if audio.audio_file:
            audio.audio_file.delete()  # 删除音频文件

        # 删除 Audio 记录
        audio.delete()

        return redirect('/audio/list_audio')  # 重定向到音频列表页面
    except Audio.DoesNotExist:
        # 如果 Audio 对象不存在，返回错误信息或页面
        return HttpResponse("Audio not found", status=404)

# def delete_audio2(request):
#     if request.method == 'POST':
#         id = request.POST.get('id')
#
#         if not id:
#             return JsonResponse({"error": "Missing 'id' parameter"}, status=400)
#
#         try:
#             audio = Audio.objects.get(id=id)
#
#             # 删除关联的音频文件
#             if audio.audio_file:
#                 audio.audio_file.delete()
#
#             # 删除 Audio 记录
#             audio.delete()
#
#             return JsonResponse({"message": "Audio deleted successfully"}, status=200)
#         except Audio.DoesNotExist:
#             return JsonResponse({"error": "Audio not found"}, status=404)
#
#     return JsonResponse({"error": "Invalid request method"}, status=400)
