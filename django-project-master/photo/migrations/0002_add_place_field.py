# Generated manually

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('photo', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='image',
            name='place',
            field=models.CharField(blank=True, max_length=100, null=True),
        ),
    ] 