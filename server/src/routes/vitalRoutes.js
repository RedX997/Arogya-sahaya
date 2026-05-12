const express = require('express');
const router = express.Router();
const { getVitals, addVital } = require('../controllers/vitalController');

router.get('/:userId', getVitals);
router.post('/', addVital);

module.exports = router;
