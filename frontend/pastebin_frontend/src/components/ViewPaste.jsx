import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axios from 'axios';
import './ViewPaste.css';

const ViewPaste = () => {
    const { uniqueUrl } = useParams();
    const [paste, setPaste] = useState(null);
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState('');
    const [username, setUsername] = useState('');
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchPasteAndComments = async () => {
            try {
                // Fetch paste details
                const pasteResponse = await axios.get(`http://localhost:8080/api/paste/${uniqueUrl}?_=${new Date().getTime()}`);
                setPaste(pasteResponse.data);

                // Fetch comments only if paste is found
                if (pasteResponse.data) {
                    const commentsResponse = await axios.get(`http://localhost:8080/api/paste/${uniqueUrl}/comments`);
                    setComments(commentsResponse.data);
                }
            } catch (error) {
                setError('Paste not found');
            }
        };

        fetchPasteAndComments();
    }, [uniqueUrl]);

    const handleCommentSubmit = async () => {
        // Validation
        if (!username || !newComment) {
            alert('Username and comment content cannot be empty');
            return;
        }
        if (username.length > 15) {
            alert('Username cannot be longer than 15 characters');
            return;
        }
        if (newComment.length > 100) {
            alert('Comment cannot be longer than 100 characters');
            return;
        }

        try {
            const response = await axios.post(`http://localhost:8080/api/paste/${uniqueUrl}/comments`, {
                username: username,
                content: newComment,
            });
            setComments([...comments, response.data]);
            setNewComment('');
            setUsername('');
        } catch (error) {
            console.error('Error submitting comment:', error);
        }
    };

    // Function to safely parse dates
    const parseDate = (dateString) => {
        const date = new Date(dateString);
        return isNaN(date.getTime()) ? 'Invalid date' : date.toLocaleString();
    };

    if (error) return <div className="error">{error}</div>;

    return (
        <div className="view-container">
            {paste ? (
                <>
                    <h1 className="title">{paste.title}</h1>
                    <div className="content">{paste.content}</div>
                    <div className="details">
                        <p><strong>Username:</strong> {paste.username}</p>
                        <p><strong>Expires At:</strong> {parseDate(paste.expirationTime)}</p>
                        <p><strong>Views:</strong> {paste.viewCount}</p>
                    </div>

                    <div className="comments-section">
                        <h2>Comments</h2>
                        {comments.length > 0 ? (
                            comments.map((comment, index) => (
                                <div key={index} className="comment">
                                    <p><strong>{comment.username}</strong> at {parseDate(comment.timestamp)}</p>
                                    <p>{comment.content}</p>
                                </div>
                            ))
                        ) : (
                            <p>No comments yet.</p>
                        )}

                        <div className="comment-form">
                            <h3>Add a Comment</h3>
                            <input
                                type="text"
                                placeholder="Your name (max 25 chars)"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                maxLength="25" // Restricts the input length to 15 characters
                            />
                            <textarea
                                placeholder="Your comment (max 200 chars)"
                                value={newComment}
                                onChange={(e) => setNewComment(e.target.value)}
                                maxLength="200" // Restricts the input length to 100 characters
                            ></textarea>
                            <button onClick={handleCommentSubmit}>Submit</button>
                        </div>
                    </div>
                </>
            ) : (
                <p className="loading">Loading...</p>
            )}
        </div>
    );
};

export default ViewPaste;
