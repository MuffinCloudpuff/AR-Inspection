from django.shortcuts import render, redirect
from django.http import HttpResponse
from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.response import Response

from studydjango.models import Student


# Create your views here.
def testurl(request):
    return HttpResponse("<h1>testurl you are looking for</h1>")
def yesredirect(request,year):
    return HttpResponse(f"{year} you are looking for")

def student(request):
    return render(request,"students.html")

def info(request):
    student_name = request.POST['name']
    student_age = request.POST['age']
    student_gender = request.POST['gender']

    Student.objects.create(name=student_name, age=student_age, gender=student_gender)
    print(student_name,student_age,student_gender)
    return render(request,"over.html")

def student_list_view(request):
    students = Student.objects.all()
    context = {'students': students}
    return render(request,"list.html",context)

@api_view(['GET'])
def student_list(request):
    students = Student.objects.all().values('id', 'name', 'age', 'gender')
    return Response(list(students))

@api_view(['DELETE'])
def delete_student(request, student_id):
    try:
        student = Student.objects.get(id=student_id)
        student.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
    except Student.DoesNotExist:
        return Response(status=status.HTTP_404_NOT_FOUND)


def delete_student2(request):
    id = request.POST['id']
    student = Student.objects.get(id=id)
    student.delete()
    return redirect('/test/list')
