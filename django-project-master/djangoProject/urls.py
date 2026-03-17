"""
URL configuration for djangoProject project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/5.1/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.contrib import admin
from django.urls import path,include
from django.http import HttpResponse
from django.shortcuts import render

from django.conf import settings
from django.conf.urls.static import static  # 用于处理静态和媒体文件

def HomeView(request):
    return render(request,'HOME.html')
urlpatterns = [
    path('admin/', admin.site.urls),
    path('audio/', include("audiorecord.urls")),
    path('test/', include("studydjango.urls")),
    path('mission/', include("mission.urls")),
    path('image/', include("photo.urls")),
    path('csrf/', include("getcsrftoken.urls")),

    path('', HomeView),
]

# 让 Django 在开发模式下正确提供媒体文件
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)