const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

// --- Vitals ---
exports.getVitals = async (req, res) => {
  const vitals = await prisma.vital.findMany({ where: { userId: req.user.id }, orderBy: { timestamp: 'desc' } });
  res.json(vitals);
};

exports.addVital = async (req, res) => {
  const { systolic, diastolic, heartRate, sugar, notes } = req.body;
  const vital = await prisma.vital.create({
    data: { userId: req.user.id, systolic, diastolic, heartRate, sugar: parseFloat(sugar), notes }
  });
  res.status(201).json(vital);
};

// --- Medicines ---
exports.getMedicines = async (req, res) => {
  const medicines = await prisma.medicine.findMany({ where: { userId: req.user.id }, orderBy: { createdAt: 'desc' } });
  res.json(medicines);
};

exports.addMedicine = async (req, res) => {
  const { name, dosage, time, totalTablets, remainingTablets, frequency, pillImageUri } = req.body;
  const medicine = await prisma.medicine.create({
    data: { 
      userId: req.user.id, 
      name, 
      dosage, 
      time: new Date(time), 
      totalTablets, 
      remainingTablets, 
      frequency, 
      pillImageUri 
    }
  });
  res.status(201).json(medicine);
};

// --- Symptoms ---
exports.getSymptoms = async (req, res) => {
  const symptoms = await prisma.symptom.findMany({ where: { userId: req.user.id }, orderBy: { date: 'desc' } });
  res.json(symptoms);
};

exports.addSymptom = async (req, res) => {
  const { date, notes, tags } = req.body;
  const symptom = await prisma.symptom.create({
    data: { userId: req.user.id, date: new Date(date), notes, tags }
  });
  res.status(201).json(symptom);
};

// --- Medical Records ---
exports.getRecords = async (req, res) => {
  const records = await prisma.medicalRecord.findMany({ where: { userId: req.user.id }, orderBy: { date: 'desc' } });
  res.json(records);
};

exports.addRecord = async (req, res) => {
  const { title, category, date, fileUri, notes } = req.body;
  const record = await prisma.medicalRecord.create({
    data: { userId: req.user.id, title, category, date: new Date(date), fileUri, notes }
  });
  res.status(201).json(record);
};

// --- Appointments ---
exports.getAppointments = async (req, res) => {
  const appointments = await prisma.appointment.findMany({ where: { userId: req.user.id }, orderBy: { dateTime: 'asc' } });
  res.json(appointments);
};

exports.addAppointment = async (req, res) => {
  const { doctorName, dateTime, notes } = req.body;
  const appointment = await prisma.appointment.create({
    data: { userId: req.user.id, doctorName, dateTime: new Date(dateTime), notes }
  });
  res.status(201).json(appointment);
};

// --- Family Members ---
exports.getFamily = async (req, res) => {
  const family = await prisma.familyMember.findMany({ where: { userId: req.user.id } });
  res.json(family);
};

exports.addFamilyMember = async (req, res) => {
  const { name, phoneNumber, relation } = req.body;
  const member = await prisma.familyMember.create({
    data: { userId: req.user.id, name, phoneNumber, relation }
  });
  res.status(201).json(member);
};
