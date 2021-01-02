if [ "$APP_FUNCTION" == "worker" ]
then
  echo "Running worker..."
  python worker.py
else
  echo "Running api..."
  if [ "$APP_ENV" == "prod" ]
  then
    gunicorn -b 0.0.0.0:5000 --access-logfile - app:app
  else
    python app.py
  fi
fi
