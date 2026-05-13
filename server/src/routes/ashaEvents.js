const express = require('express');
const router = express.Router();
const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

// In-memory fallback if DB is not reachable or table missing
let mockAshaEvents = [
  {
    id: "mock-1",
    title: "Polio Vaccination Drive",
    date: Date.now(),
    location: "Primary Health Center",
    description: "Mandatory vaccination for children under 5.",
    type: "CAMP"
  }
];

// GET all ASHA Events
router.get('/', async (req, res) => {
  try {
    const events = await prisma.ashaEvent.findMany({
      orderBy: { date: 'asc' }
    });
    
    const serializedEvents = events.map(event => ({
      ...event,
      date: Number(event.date)
    }));
    
    res.json(serializedEvents);
  } catch (error) {
    console.error("Cloud DB fetch failed, falling back to mock:", error.message);
    res.json(mockAshaEvents);
  }
});

// POST a new ASHA Event
router.post('/', async (req, res) => {
  console.log("Adding new ASHA event:", req.body);
  try {
    const { title, date, location, description, type, latitude, longitude, time, organizer } = req.body;
    
    const newEvent = await prisma.ashaEvent.create({
      data: {
        title,
        date: BigInt(date),
        location,
        description,
        type,
        latitude: parseFloat(latitude) || null,
        longitude: parseFloat(longitude) || null,
        time,
        organizer
      }
    });
    
    res.status(201).json({
      ...newEvent,
      date: Number(newEvent.date)
    });
  } catch (error) {
    console.error("Cloud DB save failed, saving to mock memory:", error.message);
    const mockEvent = {
      ...req.body,
      id: "mock-" + Date.now(),
      date: parseInt(req.body.date) || Date.now()
    };
    mockAshaEvents.push(mockEvent);
    res.status(201).json(mockEvent);
  }
});

module.exports = router;
