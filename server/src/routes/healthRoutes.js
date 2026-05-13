const express = require('express');
const router = express.Router();
const healthController = require('../controllers/healthController');
const auth = require('../middleware/auth');

// Apply auth middleware to all routes
router.use(auth);

// Vitals
router.get('/vitals', healthController.getVitals);
router.post('/vitals', healthController.addVital);

// Medicines
router.get('/medicines', healthController.getMedicines);
router.post('/medicines', healthController.addMedicine);

// Symptoms
router.get('/symptoms', healthController.getSymptoms);
router.post('/symptoms', healthController.addSymptom);

// Records
router.get('/records', healthController.getRecords);
router.post('/records', healthController.addRecord);

// Appointments
router.get('/appointments', healthController.getAppointments);
router.post('/appointments', healthController.addAppointment);

// Family
router.get('/family', healthController.getFamily);
router.post('/family', healthController.addFamilyMember);

module.exports = router;
