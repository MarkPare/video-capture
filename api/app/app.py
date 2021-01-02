from flask import Flask, request, jsonify, stream_with_context, send_from_directory, make_response
from utils import unzip_all, convert_images_to_movie
import os
import json

UPLOAD_FOLDER = './uploads'
if not os.path.isdir(UPLOAD_FOLDER):
    os.mkdir(UPLOAD_FOLDER)
app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

@app.route('/hello')
def hello():
	return 'Hello World!'

@app.route('/sessions/<session_id>/manifest')
def get_manifest(session_id):
    path = os.path.join(app.config['UPLOAD_FOLDER'], session_id)
    response = make_response(send_from_directory(path, 'manifest.json'))
    # TODO: move cors ops to middleware
    response.headers.add('Access-Control-Allow-Origin', '*')
    return response

@app.route('/sessions/<session_id>/chunks/<chunk_name>')
def get_chunk(session_id, chunk_name):
    path = os.path.join(app.config['UPLOAD_FOLDER'], session_id, 'videos')
    response = make_response(send_from_directory(path, chunk_name))
    response.headers.add('Access-Control-Allow-Origin', '*')
    return response

@app.route('/upload/<session_id>', methods=['POST'])
def upload(session_id):
    file = request.files['file']
    filename = file.filename
    session_dir = os.path.join(app.config['UPLOAD_FOLDER'], session_id)
    zipped_dir = os.path.join(session_dir, 'zipped')
    images_dir = os.path.join(session_dir, 'images')
    videos_dir = os.path.join(session_dir, 'videos')
    tmp_dir = os.path.join(session_dir, 'tmp')
    if not os.path.exists(session_dir):
        os.mkdir(session_dir)
        os.mkdir(zipped_dir)
        os.mkdir(images_dir)
        os.mkdir(videos_dir)
        os.mkdir(tmp_dir)
        manifestPath = os.path.join(session_dir, 'manifest.json')
        with open(manifestPath, 'w+') as outFile:
            config = {
                'session_id': session_id,
                'status': 'processing',
                'chunks': [],
            }
            json.dump(config, outFile, indent=2, sort_keys=True)

    file.save(os.path.join(zipped_dir, filename))

    response = jsonify({'success': True})
    response.headers.add('Access-Control-Allow-Origin', '*')
    return response

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
