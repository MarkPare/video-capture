import React from 'react';
import VideoBrowser from './VideoBrowser';
import {Route, Switch} from 'react-router';

interface Props {}

const App: React.FC<Props> = () => {
  return (
    <div className='app'>
      <Switch>
        <Route
          path='/'
          component={VideoBrowser}
        />
      </Switch>
    </div>
  )
}

export default App;
