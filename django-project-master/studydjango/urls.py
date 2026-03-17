from django.urls import path
from django.http import HttpResponse
from . import views
from .views import student_list, delete_student

urlpatterns = [
    path('', views.testurl),
    # path('<year>/', views.yesredirect),
    path('students/', views.student),
    path('submit_student_info', views.info),
    path('list/', views.student_list_view),
    path('delete/', views.delete_student2),
    path('api/students/', student_list, name='student-list'),
    path('api/students/<int:student_id>/', delete_student, name='delete-student'),
]