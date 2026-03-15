const express = require('express');
const router = express.Router();
const multer = require('multer');
const examEligibilityController = require('../controllers/examEligibilityController');

// Configure multer for memory storage (file stored in buffer)
const storage = multer.memoryStorage();

// File filter to accept only Excel files
const fileFilter = (req, file, cb) => {
    const allowedMimeTypes = [
        'application/vnd.ms-excel', // .xls
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // .xlsx
        'application/vnd.ms-excel.sheet.macroEnabled.12' // .xlsm
    ];

    if (allowedMimeTypes.includes(file.mimetype)) {
        cb(null, true);
    } else {
        cb(new Error('Invalid file type. Only Excel files (.xls, .xlsx) are allowed.'), false);
    }
};

// Initialize multer
const upload = multer({
    storage: storage,
    fileFilter: fileFilter,
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB max file size
    }
});

/**
 * @route   POST /api/university/upload-exam-eligibility
 * @desc    Upload Excel file with eligible students for exam session
 * @access  Private (University Admin only)
 * @body    multipart/form-data with 'file' field
 */
router.post(
    '/upload-exam-eligibility',
    // authMiddleware,           // Uncomment when auth is integrated
    // universityAdminOnly,      // Uncomment when role middleware is integrated
    upload.single('file'),       // 'file' is the field name
    examEligibilityController.uploadExamEligibility
);

/**
 * @route   GET /api/university/exam-eligibility/:examSession
 * @desc    Get all eligible students for a specific exam session
 * @access  Private (University Admin only)
 */
router.get(
    '/exam-eligibility/:examSession',
    // authMiddleware,           // Uncomment when auth is integrated
    // universityAdminOnly,      // Uncomment when role middleware is integrated
    examEligibilityController.getEligibleStudents
);

// Error handling middleware for multer errors
router.use((error, req, res, next) => {
    if (error instanceof multer.MulterError) {
        if (error.code === 'LIMIT_FILE_SIZE') {
            return res.status(400).json({
                success: false,
                message: 'File size too large. Maximum size is 5MB.'
            });
        }
        return res.status(400).json({
            success: false,
            message: error.message
        });
    } else if (error) {
        return res.status(400).json({
            success: false,
            message: error.message
        });
    }
    next();
});

module.exports = router;
