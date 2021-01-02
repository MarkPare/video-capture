import React from 'react';
import '../scss/VideoBrowser.scss';
import { RouteComponentProps } from 'react-router-dom';
import { fetchChunk, fetchManifest } from '../utils';

/**
 * TODO: add types here and make sessionId dynamically
 * captured (e.g. from url param)
 */
const sessionId = '1609320106';

class VideoBrowser extends React.Component<Props, State> {
  video = React.createRef<HTMLVideoElement>()
  checker: any = null
  timer: any = null

  state = {
    sessionData: null,
    time: 0,
    chunksDownloaded: [],
    currentChunk: 0,
    chunks: [],
    queue: [],
  }
  async componentDidMount() {
    this.video.current!.addEventListener('ended', () => {
      this.onVideoEnded()
    })
    const sessionData = await fetchManifest(sessionId)
    this.setState({sessionData});
    const initialChunks = sessionData.chunks.slice(0, 4);
    const promises = initialChunks.map(async (chunk: any) => {
      return await this.downloadChunk(chunk);
    })
    const arrayBuffers = await Promise.all(promises);
    this.changeSrc(arrayBuffers[0]);
    this.checker = setInterval(this.checkManifest, 5000)
  }

  checkManifest = async () => {
    const {chunksDownloaded} = this.state;
    const sessionData = await fetchManifest(sessionId)
    this.setState({sessionData})
    const allChunks = sessionData.chunks as any
    const nextChunkNotDownloaded = allChunks.filter((c: never) => {
      return !chunksDownloaded.includes(c)
    })
    if (nextChunkNotDownloaded.length) {
      this.downloadChunk(nextChunkNotDownloaded[0])
    } else {
      console.log('video ended, no more chunks')
    }
  }

  onVideoEnded = () => {
    const {currentChunk, chunks} = this.state;
    const nextChunkIndex = currentChunk + 1;
    const nextChunk = chunks[nextChunkIndex];
    if (!nextChunk) return;
    this.changeSrc(nextChunk)
    this.setState({currentChunk: nextChunkIndex})
  }

  changeSrc = (src: any) => {
    const video = this.video.current!
    video.src = URL.createObjectURL(new Blob([src]));
    video.play();
  }

  downloadChunk = async (chunkName: string) => {
    const arrayBuffer = await fetchChunk(sessionId, chunkName)
    const prevChunksDownloaded = this.state.chunksDownloaded;
    const chunksDownloaded = [...prevChunksDownloaded, chunkName];
    const chunks = [...this.state.chunks, arrayBuffer]
    this.setState({chunksDownloaded, chunks})
    return arrayBuffer
  }

  render() {
    const {sessionData} = this.state;
    const cls = `video-browser`
    const sessionId = sessionData !== null
      ? (sessionData as any).sessionId
      : ''
    return (
      <div className={cls}>
        Video {sessionId}
        <video
          className='video'
          ref={this.video}
          autoPlay
          controls
        ></video>
      </div>
    );
  }
}

interface State {
  sessionData: any
  time: number
  chunksDownloaded: string[]
  currentChunk: any
  chunks: any
  queue: any
}
interface Props extends RouteComponentProps {}

export default VideoBrowser;
