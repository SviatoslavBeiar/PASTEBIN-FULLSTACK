import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import CreatePaste from './components/CreatePaste';
import ViewPaste from './components/ViewPaste';

const App = () => {
    return (
        <Router>
            <div className="App">
                <Routes>
                    <Route path="/" element={<CreatePaste />} />
                    <Route path="/paste/:uniqueUrl" element={<ViewPaste />} />

                </Routes>
            </div>
        </Router>
    );
}

export default App;
