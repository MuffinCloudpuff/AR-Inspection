from django.urls import path
from . import views
from .views import get_csrf_token

urlpatterns = [
    path('', views.testurl),
    # path('<year>/', views.yesredirect),
    path('api/get-csrf-token/', get_csrf_token, name='get-csrf-token')
]