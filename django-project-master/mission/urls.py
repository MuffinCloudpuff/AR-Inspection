from django.urls import path
from django.http import HttpResponse
from . import views
from .views import mission_list, delete_mission
from .views import mission_info, mission_list_page, mission_by_name
from .views import complete_mission, unfinished_missions

urlpatterns = [
    path('', views.testurl),
    # path('<year>/', views.yesredirect),
    path('missions/', views.mission),
    path('submit_mission_info', views.info),
    path('mission_info/', mission_info, name='mission_info'),

    path('list_mission/', views.mission_list_view),
    path('list_mission_page/', mission_list_page, name='mission_list_page'),

    path('delete/', views.delete_mission2),
    path('api/missions/', mission_list, name='mission-list'),
    path('api/missions/<int:mission_id>/', delete_mission, name='delete-mission'),

    path('api/missions/complete/<int:mission_id>/', complete_mission, name='complete-mission'),
    path('api/missions/unfinished/', unfinished_missions, name='unfinished-missions'),

    path('mission_by_name/<str:name>', mission_by_name, name='mission_by_name'),
    
    path('detail/<int:mission_id>/', views.mission_detail, name='mission-detail'),
    path('generate_report/<int:mission_id>/', views.generate_mission_report, name='generate_mission_report'),
    path('report/<int:mission_id>/', views.mission_report, name='mission_report'),
]