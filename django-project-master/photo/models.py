from django.db import models
from datetime import datetime
import os

# 修改为图片文件的上传路径
def image_upload_path(instance, filename):
    """ 生成图片文件的上传路径，例如：images/smallyun_202503272140.jpg 或 images/smallyun_202503272140.png """
    if not instance.name:
        instance.name = "default"  # 避免 `name` 为空导致路径错误

    time_code = datetime.now().strftime("%Y%m%d%H%M")
    ext = os.path.splitext(filename)[1]  # 获取文件扩展名

    # 如果没有扩展名，默认为 .jpg
    if not ext:
        ext = ".jpg"

    return f"images/{instance.name}_{time_code}{ext}"

def default_time_code():
    return datetime.now().strftime("%Y%m%d%H%M")  # 使用普通函数

class Image(models.Model):
    name = models.CharField(max_length=100)  # 存储姓名
    time_code = models.CharField(max_length=12, default=default_time_code)  # 代替 lambda
    image_file = models.ImageField(upload_to=image_upload_path)  # 存储图片文件
    place = models.CharField(max_length=100, blank=True, null=True)  # 存储位置信息，从二维码中提取
    task_name = models.CharField(max_length=100, blank=True, null=True)  # 存储任务名称
    executor = models.CharField(max_length=100, blank=True, null=True)  # 存储执行人

    def save(self, *args, **kwargs):
        if not self.name:
            self.name = "default"  # 确保 name 不能为空
        
        # 如果executor为空，则默认使用name值
        if not self.executor:
            self.executor = self.name
            
        super(Image, self).save(*args, **kwargs)