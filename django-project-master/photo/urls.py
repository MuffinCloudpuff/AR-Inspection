from django.urls import path
from django.http import HttpResponse
from . import views
from .views import image_list, delete_image
from .views import image_list_view, image_list_page, image_info
from .views import image_detail

urlpatterns = [
    path('', views.testurl),
    # path('<year>/', views.yesredirect),
    path('images/', views.image),  # 修改为图片上传页面
    path('submit_image_info', views.info),  # 修改为图片信息提交
    path('image_info/', image_info, name='image_info'),  # 修改为图片信息提交接口

    path('list_image/', views.image_list_view),  # 修改为图片列表页面
    path('list_image_page/', image_list_page, name='image_list_page'),  # 修改为图片列表接口
    path('detail/<int:image_id>/', image_detail, name='image-detail'),  # 图片详情页面

    path('delete/', views.delete_image2),  # 修改为删除图片
    path('api/images/', image_list, name='image-list'),  # 修改为图片列表 API
    path('api/images/<int:image_id>/', delete_image, name='delete-image'),  # 修改为删除图片 API
]