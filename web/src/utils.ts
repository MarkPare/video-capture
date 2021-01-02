import fs from 'fs';
import {parse as qsParse} from 'qs';

const API_URL = 'http://localhost:5000';

export interface SessionData {
  sessionId: string
  chunks: string[]
}

const mapManifestResponse = (data: any) => {
  return {
    ...data,
    sessionId: data.session_id,
  }
}

export const fetchManifest = async (sessionId: string) => {
  const url = `${API_URL}/sessions/${sessionId}/manifest`;
  return fetch(url)
  .then(response => response.json())
  .then(data => {
    return mapManifestResponse(data);
  })
  .catch(() => {
    return Promise.reject()
  })
}

export const fetchChunk = async (sessionId: string, chunkName: string) => {
  const url = `${API_URL}/sessions/${sessionId}/chunks/${chunkName}`;
  return fetch(url)
  .then(response => {
    return response.arrayBuffer()
  })
  .catch(() => {
    return Promise.reject()
  })
}

export const parseUrlForKey = (key: string, url: string): string | undefined => {
  const parsed = qsParse(url, {ignoreQueryPrefix: true});
  return parsed[key] as string | undefined;
}
