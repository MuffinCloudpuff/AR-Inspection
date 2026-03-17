from django.urls import path
from django.http import HttpResponse
from . import views
from .views import audio_list, delete_audio
from .views import audio_list_view, audio_list_page, audio_info

urlpatterns = [
    path('', views.testurl),
    # path('<year>/', views.yesredirect),
    path('audios/', views.audio),
    path('submit_audio_info', views.info),
    path('audio_info/', audio_info, name='audio_info'),

    path('list_audio/', views.audio_list_view),  # 渲染 HTML
    path('list_audio_page/', audio_list_page, name='audio_list_page'),

    path('delete/', views.delete_audio2),
    path('api/audios/', audio_list, name='audio-list'),
    path('api/audios/<int:audio_id>/', delete_audio, name='delete-audio'),
]