from django.shortcuts import render
from django.http import JsonResponse
from django.middleware.csrf import get_token
from django.http import HttpResponse
# Create your views here.

def testurl(request):
    return HttpResponse("<h1>testurl you are looking for</h1>")
def yesredirect(request,year):
    return HttpResponse(f"{year} you are looking for")
def get_csrf_token(request):
    return JsonResponse({'csrfToken': get_token(request)})