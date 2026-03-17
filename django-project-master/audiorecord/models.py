from django.db import models

# Create your models here.
from datetime import datetime
import os


# def audio_upload_path(instance, filename):
#     """ 生成音频文件的上传路径，例如：audio_files/smallyun_202503272140.mp3 """
#     time_code = datetime.now().strftime("%Y%m%d%H%M")
#     ext = os.path.splitext(filename)[1]  # 获取文件扩展名（如 .mp3）
#     return f"audio_files/{instance.name}_{time_code}{ext}"
def audio_upload_path(instance, filename):
    """ 生成音频文件的上传路径，例如：audio_files/smallyun_202503272140.mp3 """
    if not instance.name:
        instance.name = "default"  # 避免 `name` 为空导致路径错误

    time_code = datetime.now().strftime("%Y%m%d%H%M")
    ext = os.path.splitext(filename)[1] or ".mp3"  # 获取文件扩展名（默认 .mp3）

    return f"audio_files/{instance.name}_{time_code}{ext}"


def default_time_code():
    return datetime.now().strftime("%Y%m%d%H%M")  #使用普通函数
class Audio(models.Model):
    name = models.CharField(max_length=100)  # 存储姓名
    time_code = models.CharField(max_length=12, default=default_time_code)  #代替 lambda
    audio_file = models.FileField(upload_to=audio_upload_path)  # 存储 .mp3 文件

    def save(self, *args, **kwargs):
        if not self.name:
            self.name = "default"  # 确保 name 不能为空
        super(Audio, self).save(*args, **kwargs)