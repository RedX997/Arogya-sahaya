const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

const path = require('path');

// Middleware
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));
app.use(express.static('public'));

// Download Route
app.get('/download', (req, res) => {
  res.download(path.join(__dirname, '../public/arogyasahaya.apk'));
});

// Basic Health Check
app.get('/', (req, res) => {
  res.json({ message: 'Arogya Sahaya Cloud API is live!', status: 'HEALTHY' });
});

// Routes
app.use('/api/auth', require('./routes/authRoutes'));
app.use('/api/health', require('./routes/healthRoutes'));
app.use('/api/asha-events', require('./routes/ashaEvents'));

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
