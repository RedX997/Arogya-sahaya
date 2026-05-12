const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

// Basic Health Check
app.get('/', (req, res) => {
  res.json({ message: 'Arogya Sahaya Cloud API is live!', status: 'HEALTHY' });
});

// Routes
app.use('/api/vitals', require('./routes/vitalRoutes'));
// app.use('/api/medicines', require('./routes/medicineRoutes'));

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
