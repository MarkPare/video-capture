import time
import json
import os
import subprocess
import shutil
from utils import convert_images_to_movie, unzip_file


UPLOADS_DIR = './uploads'
BATCH_SIZE = 50
EXTENSION = 'webm'

def get_manifest_path(session_id):
    return os.path.join(UPLOADS_DIR, session_id, 'manifest.json')

def get_tmp_path(session_id):
    return os.path.join(UPLOADS_DIR, session_id, 'tmp')

def get_tmp_batch_path(batch_id, session_id):
    return os.path.join(UPLOADS_DIR, session_id, 'tmp', batch_id)

def get_zipped_path(session_id):
    return os.path.join(UPLOADS_DIR, session_id, 'zipped')

def get_zip_file_path(file_name, session_id):
    return os.path.join(UPLOADS_DIR, session_id, 'zipped', file_name)
 
def get_image_path(file_name, session_id):
    return os.path.join(UPLOADS_DIR, session_id, 'images', file_name)

def get_images_path(session_id):
    return os.path.join(UPLOADS_DIR, session_id, 'images')

def get_session_path(session_id):
    return os.path.join(UPLOADS_DIR, session_id)

def get_videos_path(session_id):
    return os.path.join(UPLOADS_DIR, session_id, 'videos')

def get_video_path(file_name, session_id):
    return os.path.join(UPLOADS_DIR, session_id, 'videos', file_name)

def does_file_exist(file_path):
    return os.path.isfile(file_path)

def make_padded_file_name(image_id):
    max_length = 10
    current_length = len(image_id)
    diff = max_length - current_length
    zeroes = "".join(['0' for x in range(diff)])
    return "{}{}.jpg".format(zeroes, image_id)

def file_name_to_id(file_name):
    name_padded = file_name.split('.jpg')[0]
    name = int(name_padded)
    return name

def get_next_batch(config):
    session_id = config.get('session_id', None)
    is_uploaded = config.get('status', None) == 'uploaded'
    batch_size = BATCH_SIZE
    index = 0
    last_file_processed = config.get('last_file_processed', None)
    if last_file_processed != None:
        index = file_name_to_id(last_file_processed) + 1

    files_to_be_processed = []
    top_value = index + batch_size
    for i in range(index, top_value):
        files_to_be_processed.append(make_padded_file_name(str(i)))
    if len(files_to_be_processed) == 0:
        return None
    # Make sure all files in batch exist
    last_file_name = files_to_be_processed[len(files_to_be_processed) - 1]
    last_file_path = get_image_path(last_file_name, session_id)
    if not does_file_exist(last_file_path):
        return None
    return files_to_be_processed
        
def process_batch(batch, batch_id, config):
    session_id = config.get('session_id')
    tmp_batch_path = get_tmp_batch_path(batch_id, session_id)
    os.mkdir(tmp_batch_path)
    for file_name in batch:
        src = get_image_path(file_name, session_id)
        dest = os.path.join(tmp_batch_path)
        shutil.copy(src, dest)
    batch_name = "{}.{}".format(batch_id, EXTENSION)
    out_file_path = get_video_path(batch_name, session_id)
    convert_images_to_movie(tmp_batch_path, out_file_path)
    shutil.rmtree(tmp_batch_path)
    return batch_name

def save_manifest(session_id, config):
    path = get_manifest_path(session_id)
    with open(path, 'w+') as file:
        json.dump(config, file, indent=2, sort_keys=True)


def check_for_pending_sessions():
    sessions = os.listdir(UPLOADS_DIR)
    results = {}
    for session_id in sessions:
        manifest_path = os.path.join(UPLOADS_DIR, session_id, 'manifest.json')
        if os.path.isfile(manifest_path) == False:
            continue
        with open(manifest_path, 'r') as file:
            config = json.loads(file.read())
        results[session_id] = config
    return results

def work():
    while(True):
        print('Working...')
        session_data = check_for_pending_sessions()
        print('session_data', session_data)
        for session_id in session_data.keys():
            config = session_data[session_id]
            # Check for unzipped files and unzip them
            zipped_dir = get_zipped_path(session_id)
            count = len(os.listdir(zipped_dir))
            files_per_chunk = 100
            for zip_file in os.listdir(zipped_dir):
                if not zip_file.endswith('.zip'): continue
                full_path = get_zip_file_path(zip_file, session_id)
                unzip_file(full_path, get_images_path(session_id))
                os.remove(full_path)
            # TODO: probably do filtering on status in
            # check_for_pending_sessions func
            if config.get('status') != "processing": continue
            maxBatches = 3
            next_config = {
                **config,
            }
            for i in range(0, maxBatches):
                batch = get_next_batch(next_config)
                if batch == None: continue
                last_batch_id = next_config.get('last_batch_id', None)
                next_batch_id = '0' if last_batch_id == None else str(int(last_batch_id) + 1)
                chunk_name = process_batch(batch, next_batch_id, next_config)
                chunks = next_config.get('chunks')
                chunks.append(chunk_name)
                next_config = {
                    **next_config,
                    'last_batch_id': next_batch_id,
                    'last_file_processed': batch[len(batch) - 1],
                    'chunks': chunks,
                }
            save_manifest(session_id, next_config)

        print('-' * 10)
        print('')
        time.sleep(5)

if __name__ == '__main__':
    work()
