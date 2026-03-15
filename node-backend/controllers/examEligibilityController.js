const xlsx = require('xlsx');
const db = require('../config/db'); // Adjust path as per your db config file

/**
 * Controller: Upload Exam Eligibility Excel
 * POST /api/university/upload-exam-eligibility
 */
exports.uploadExamEligibility = async (req, res) => {
    try {
        // Check if file is uploaded
        if (!req.file) {
            return res.status(400).json({
                success: false,
                message: 'No file uploaded. Please upload an Excel file.'
            });
        }

        // Parse Excel file
        const workbook = xlsx.read(req.file.buffer, { type: 'buffer' });
        const sheetName = workbook.SheetNames[0];
        const worksheet = workbook.Sheets[sheetName];
        const data = xlsx.utils.sheet_to_json(worksheet);

        if (!data || data.length === 0) {
            return res.status(400).json({
                success: false,
                message: 'Excel file is empty or invalid.'
            });
        }

        // Validate required columns
        const requiredColumns = ['prn_number', 'full_name', 'semester', 'exam_session'];
        const firstRow = data[0];
        const missingColumns = requiredColumns.filter(col => !(col in firstRow));

        if (missingColumns.length > 0) {
            return res.status(400).json({
                success: false,
                message: `Missing required columns: ${missingColumns.join(', ')}`
            });
        }

        // Statistics
        let totalRecords = data.length;
        let insertedRecords = 0;
        let skippedDuplicates = 0;
        const errors = [];

        // Get university_id from authenticated admin (adjust based on your auth middleware)
        const universityId = req.user?.university_id || req.body.university_id || 1; // Default to 1 if not found

        // Process each row
        for (let i = 0; i < data.length; i++) {
            const row = data[i];

            try {
                // Convert eligible_subjects from comma-separated string to JSON array
                let eligibleSubjects = [];
                if (row.eligible_subjects && typeof row.eligible_subjects === 'string') {
                    eligibleSubjects = row.eligible_subjects
                        .split(',')
                        .map(subject => subject.trim())
                        .filter(subject => subject.length > 0);
                } else if (Array.isArray(row.eligible_subjects)) {
                    eligibleSubjects = row.eligible_subjects;
                }

                // Prepare data for insertion
                const studentData = {
                    university_id: universityId,
                    prn_number: row.prn_number?.toString().trim(),
                    full_name: row.full_name?.toString().trim(),
                    semester: row.semester?.toString().trim() || null,
                    exam_session: row.exam_session?.toString().trim(),
                    eligible_subjects: JSON.stringify(eligibleSubjects),
                    fee_status: row.fee_status?.toString().trim() || 'Pending',
                    eligibility_status: row.eligibility_status?.toString().trim() || 'Eligible'
                };

                // Validate mandatory fields
                if (!studentData.prn_number || !studentData.full_name || !studentData.exam_session) {
                    errors.push({
                        row: i + 2, // Excel row (accounting for header)
                        error: 'Missing required fields (prn_number, full_name, or exam_session)'
                    });
                    skippedDuplicates++;
                    continue;
                }

                // Insert into database (skip if duplicate)
                const insertQuery = `
                    INSERT INTO exam_eligible_students 
                    (university_id, prn_number, full_name, semester, exam_session, eligible_subjects, fee_status, eligibility_status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                `;

                const values = [
                    studentData.university_id,
                    studentData.prn_number,
                    studentData.full_name,
                    studentData.semester,
                    studentData.exam_session,
                    studentData.eligible_subjects,
                    studentData.fee_status,
                    studentData.eligibility_status
                ];

                await db.query(insertQuery, values);
                insertedRecords++;

            } catch (dbError) {
                // Check if it's a duplicate entry error
                if (dbError.code === 'ER_DUP_ENTRY') {
                    skippedDuplicates++;
                } else {
                    errors.push({
                        row: i + 2,
                        error: dbError.message
                    });
                    skippedDuplicates++;
                }
            }
        }

        // Return response
        return res.status(200).json({
            success: true,
            message: 'Excel file processed successfully',
            data: {
                total_records: totalRecords,
                inserted_records: insertedRecords,
                skipped_duplicates: skippedDuplicates,
                errors: errors.length > 0 ? errors : undefined
            }
        });

    } catch (error) {
        console.error('Error in uploadExamEligibility:', error);
        return res.status(500).json({
            success: false,
            message: 'Server error while processing Excel file',
            error: error.message
        });
    }
};

/**
 * Controller: Get all eligible students for an exam session
 * GET /api/university/exam-eligibility/:examSession
 */
exports.getEligibleStudents = async (req, res) => {
    try {
        const { examSession } = req.params;
        const universityId = req.user?.university_id || req.query.university_id || 1;

        const query = `
            SELECT * FROM exam_eligible_students 
            WHERE exam_session = ? AND university_id = ?
            ORDER BY created_at DESC
        `;

        const [results] = await db.query(query, [examSession, universityId]);

        // Parse JSON eligible_subjects for each record
        const formattedResults = results.map(record => ({
            ...record,
            eligible_subjects: JSON.parse(record.eligible_subjects || '[]')
        }));

        return res.status(200).json({
            success: true,
            count: formattedResults.length,
            data: formattedResults
        });

    } catch (error) {
        console.error('Error in getEligibleStudents:', error);
        return res.status(500).json({
            success: false,
            message: 'Server error while fetching eligible students',
            error: error.message
        });
    }
};
