import os
import zipfile
import subprocess

def call_shell(command):
  subprocess.check_call(command, shell=True, stderr=subprocess.STDOUT)

def convert_images_to_movie(images_dir, out_path):
    input_pattern = "{}/*.jpg".format(images_dir)
    cmd = "ffmpeg -framerate 5 -pattern_type glob -i '{}' -c:v libvpx-vp9 -pix_fmt yuva420p -f webm -auto-alt-ref 0 {}".format(input_pattern, out_path)
    call_shell(cmd)

def unzip_file(source, dest):
    with zipfile.ZipFile(source, 'r') as zip_file:
        zip_file.extractall(dest)

def unzip_all(source, dest):
    files = os.listdir(source)
    for zipped in files:
        zip_source = os.path.join(source, zipped)
        unzip_file(zip_source, dest)

def get_id():
    return str(uuid4())
