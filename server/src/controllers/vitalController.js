const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

exports.getVitals = async (req, res) => {
  try {
    const { userId } = req.params;
    const vitals = await prisma.vital.findMany({
      where: { userId },
      orderBy: { timestamp: 'desc' }
    });
    res.json(vitals);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch vitals' });
  }
};

exports.addVital = async (req, res) => {
  try {
    const { userId, systolic, diastolic, heartRate, sugar, notes } = req.body;
    const newVital = await prisma.vital.create({
      data: {
        userId,
        systolic,
        diastolic,
        heartRate,
        sugar,
        notes
      }
    });
    res.status(201).json(newVital);
  } catch (error) {
    res.status(500).json({ error: 'Failed to add vital log' });
  }
};
