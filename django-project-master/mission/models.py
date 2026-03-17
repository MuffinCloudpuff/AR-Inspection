from django.db import models

# Create your models here.
class Mission(models.Model):
    task = models.CharField(max_length=100)
    name = models.CharField(max_length=100)
    state = models.CharField(max_length=100)
