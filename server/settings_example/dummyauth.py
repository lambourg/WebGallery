"""
Dummy authentication package, USED FOR TESTING ENVIRONMENT ONLY !!!!!!

This package does not authenticate at ALL so will let anyone access precious
data, give access to the admin page, etc. USE WITH EXTRA CARE !
"""

from django.contrib import auth
from django.contrib.auth.models import User


class DummyAuthMiddleware(object):
    def process_request(self, request):
        user = auth.authenticate()
        if user:
            request.user = user
            auth.login(request, user)


class DummyBackend(object):
    def authenticate(self):
        try:
            user = User.objects.get(username='doe')
        except User.DoesNotExist:
            user = User(username='doe', password='')
            user.is_staff = True
            user.is_superuser = True
            user.save()

        if len(user.first_name) == 0:
            user.first_name = 'John'
            user.last_name = 'Doe'
            user.username = 'doe'
            user.email = 'john-doe@example.com'
            user.save()
        return user

    def get_user(self, user_id):
        try:
            return User.objects.get(pk=user_id)
        except User.DoesNotExist:
            return None
