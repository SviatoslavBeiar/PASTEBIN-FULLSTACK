import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './CreatePaste.css';  // Импортируем стили

const CreatePaste = () => {
    const [content, setContent] = useState('');
    const [title, setTitle] = useState('');
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');  // Новое состояние для email
    const [expirationTime, setExpirationTime] = useState('');
    const [errors, setErrors] = useState({});
    const navigate = useNavigate();

    const validate = () => {
        const errors = {};


        if (!username.trim()) {
            errors.username = 'Username is required';
        } else if (username.length > 20) {
            errors.username = 'Username must be less than 20 characters';
        }


        if (!title.trim()) {
            errors.title = 'Title is required';
        } else if (title.length > 25) {
            errors.title = 'Title must be less than 25 characters';
        }


        if (!content.trim()) {
            errors.content = 'Content is required';
        } else if (content.length > 4000) {
            errors.content = 'Content must be less than 4000 characters';
        }


        if (!email.trim()) {
            errors.email = 'Email is required';
        } else if (!/\S+@\S+\.\S+/.test(email)) {
            errors.email = 'Email is invalid';
        }


        const expirationMinutes = Number(expirationTime);
        if (!expirationTime) {
            errors.expirationTime = 'Expiration time is required';
        } else if (isNaN(expirationMinutes) || expirationMinutes < 1 || expirationMinutes > 1000) {
            errors.expirationTime = 'Expiration time must be between 1 minute and 1000 minutes';
        }

        return errors;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const validationErrors = validate();
        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors);
            return;
        }

        try {
            const response = await axios.post('http://localhost:8080/api/paste', {
                content,
                title,
                username,
                email,  // Отправляем email
                expirationTime: expirationTime ? Number(expirationTime) : null
            });
            navigate(`/paste/${response.data}`);
        } catch (error) {
            console.error('Error creating paste:', error);
        }
    };

    return (
        <div className="container">
            <form onSubmit={handleSubmit} className="form">
                <h1>Create a New Paste</h1>
                <div>
                    <label>Username</label>
                    <input
                        type="text"
                        placeholder="Username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                    />
                    {errors.username && <p className="error">{errors.username}</p>}
                </div>
                <div>
                    <label>Email</label>  {/* Новое поле ввода для email */}
                    <input
                        type="email"
                        placeholder="Email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                    />
                    {errors.email && <p className="error">{errors.email}</p>}
                </div>
                <div>
                    <label>Title</label>
                    <input
                        type="text"
                        placeholder="Title"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                    />
                    {errors.title && <p className="error">{errors.title}</p>}
                </div>
                <div>
                    <label>Expiration Time (minutes)</label>
                    <input
                        type="number"
                        placeholder="Expiration Time (minutes)"
                        value={expirationTime}
                        onChange={(e) => setExpirationTime(e.target.value)}
                    />
                    {errors.expirationTime && <p className="error">{errors.expirationTime}</p>}
                </div>
                <div>
                    <label>Content</label>
                    <textarea
                        placeholder="Content"
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                    />
                    {errors.content && <p className="error">{errors.content}</p>}
                </div>
                <button type="submit">Create Paste</button>
            </form>
        </div>
    );
};

export default CreatePaste;
