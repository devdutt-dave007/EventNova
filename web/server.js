const express = require('express');
const cors = require('cors');
const path = require('path');
const mysql = require('mysql2/promise');

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Database pool configuration
const dbConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '3306'),
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'eventnova3',
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
};

let pool;
let isDbConnected = false;

async function initDB() {
  try {
    pool = mysql.createPool(dbConfig);
    const [rows] = await pool.query('SELECT 1 as val');
    isDbConnected = true;
    console.log('[EventNova DB] Connected successfully to MySQL database "eventnova3" on port ' + dbConfig.port);
  } catch (err) {
    isDbConnected = false;
    console.error('[EventNova DB] Connection error:', err.message);
  }
}

initDB();

// Middleware to ensure DB connection is alive
app.use(async (req, res, next) => {
  if (!isDbConnected && req.path.startsWith('/api')) {
    try {
      if (!pool) pool = mysql.createPool(dbConfig);
      await pool.query('SELECT 1');
      isDbConnected = true;
    } catch (e) {
      isDbConnected = false;
      return res.status(503).json({
        success: false,
        error: 'Database connection offline. Ensure MySQL is running on port 3306.',
        details: e.message
      });
    }
  }
  next();
});

// ============================================
// AUTHENTICATION APIS
// ============================================

// POST /api/auth/login - Handles both User and Institute login
app.post('/api/auth/login', async (req, res) => {
  try {
    const { email, password, accountType } = req.body;
    if (!email || !password || !accountType) {
      return res.status(400).json({ success: false, error: 'Email, password, and account type are required.' });
    }

    if (accountType === 'INSTITUTE') {
      const [rows] = await pool.query(
        'SELECT Institute_ID, Institute_Name, Email, Address, Balance, Joining_Date FROM Institute WHERE Email = ? AND Password = ?',
        [email.trim(), password]
      );
      if (rows.length === 0) {
        return res.status(401).json({ success: false, error: 'Invalid institute credentials. Check your email and password.' });
      }
      return res.json({
        success: true,
        accountType: 'INSTITUTE',
        user: {
          id: rows[0].Institute_ID,
          name: rows[0].Institute_Name,
          email: rows[0].Email,
          address: rows[0].Address,
          balance: parseFloat(rows[0].Balance),
          role: 'INSTITUTE'
        }
      });
    } else {
      // Normal User (Student, Faculty, External)
      const [rows] = await pool.query(`
        SELECT u.User_ID, u.Name, u.Email, u.Phone, u.Role, u.Institute_ID, u.Enrollment_No, u.Employee_ID, u.Balance,
               i.Institute_Name
        FROM User u
        LEFT JOIN Institute i ON u.Institute_ID = i.Institute_ID
        WHERE u.Email = ? AND u.Password = ?
      `, [email.trim(), password]);

      if (rows.length === 0) {
        return res.status(401).json({ success: false, error: 'Invalid user credentials. Check your email and password.' });
      }

      return res.json({
        success: true,
        accountType: 'USER',
        user: {
          id: rows[0].User_ID,
          name: rows[0].Name,
          email: rows[0].Email,
          phone: rows[0].Phone,
          role: rows[0].Role,
          instituteId: rows[0].Institute_ID,
          instituteName: rows[0].Institute_Name,
          enrollmentNo: rows[0].Enrollment_No,
          employeeId: rows[0].Employee_ID,
          balance: parseFloat(rows[0].Balance)
        }
      });
    }
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// POST /api/auth/register-user - Register user with ₹500 welcome bonus & ledger entry
app.post('/api/auth/register-user', async (req, res) => {
  const connection = await pool.getConnection();
  try {
    const { name, email, password, phone, role, instituteId, enrollmentNo, employeeId } = req.body;

    if (!name || !email || !password || !phone || !role) {
      return res.status(400).json({ success: false, error: 'Name, email, password, phone, and role are required.' });
    }

    // Check email uniqueness
    const [[existing]] = await connection.query('SELECT User_ID FROM User WHERE Email = ?', [email.trim()]);
    if (existing) {
      return res.status(400).json({ success: false, error: 'This email is already registered.' });
    }

    await connection.beginTransaction();

    const WELCOME_BONUS = 500.00;
    const [userRes] = await connection.query(`
      INSERT INTO User (Name, Email, Password, Phone, Role, Institute_ID, Enrollment_No, Employee_ID, Balance)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `, [
      name.trim(), email.trim(), password, phone.trim(), role,
      instituteId ? parseInt(instituteId) : null,
      enrollmentNo || null, employeeId || null, WELCOME_BONUS
    ]);

    const newUserId = userRes.insertId;

    // Add Welcome bonus to double-entry ledger
    await connection.query(`
      INSERT INTO Wallet_Transaction (Owner_Type, Owner_ID, Entry_Type, Amount, Balance_After, Description)
      VALUES ('USER', ?, 'BONUS', ?, ?, 'Welcome bonus on registration')
    `, [newUserId, WELCOME_BONUS, WELCOME_BONUS]);

    await connection.commit();

    res.json({
      success: true,
      message: 'Account registered successfully! A welcome bonus of ₹500 has been credited to your wallet.',
      userId: newUserId
    });
  } catch (err) {
    await connection.rollback();
    res.status(500).json({ success: false, error: err.message });
  } finally {
    connection.release();
  }
});

// POST /api/auth/register-institute - Register accredited institute
app.post('/api/auth/register-institute', async (req, res) => {
  try {
    const { name, email, password, address } = req.body;

    if (!name || !email || !password || !address) {
      return res.status(400).json({ success: false, error: 'All fields are required.' });
    }

    if (!email.endsWith('.edu.in')) {
      return res.status(400).json({ success: false, error: 'Institute email domain must end with .edu.in' });
    }

    const [[existing]] = await pool.query('SELECT Institute_ID FROM Institute WHERE Email = ?', [email.trim()]);
    if (existing) {
      return res.status(400).json({ success: false, error: 'This institute email is already registered.' });
    }

    const [result] = await pool.query(`
      INSERT INTO Institute (Institute_Name, Email, Password, Address, Balance)
      VALUES (?, ?, ?, ?, 0.00)
    `, [name.trim(), email.trim(), password, address.trim()]);

    res.json({
      success: true,
      message: 'Institute registered successfully.',
      instituteId: result.insertId
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// ============================================
// SYSTEM STATUS & OVERVIEW
// ============================================

app.get('/api/status', async (req, res) => {
  try {
    const [[{ version }]] = await pool.query('SELECT VERSION() as version');
    const [[{ dbName }]] = await pool.query('SELECT DATABASE() as dbName');
    res.json({
      success: true,
      connected: isDbConnected,
      database: dbName,
      mysqlVersion: version,
      timestamp: new Date().toISOString()
    });
  } catch (err) {
    res.json({ success: false, connected: false, error: err.message });
  }
});

app.get('/api/overview', async (req, res) => {
  try {
    const [[{ totalEvents }]] = await pool.query('SELECT COUNT(*) as totalEvents FROM Event');
    const [[{ openEvents }]] = await pool.query("SELECT COUNT(*) as openEvents FROM Event WHERE Status = 'OPEN'");
    const [[{ totalInstitutes }]] = await pool.query('SELECT COUNT(*) as totalInstitutes FROM Institute');
    const [[{ totalUsers }]] = await pool.query('SELECT COUNT(*) as totalUsers FROM User');
    const [[{ totalRegistrations }]] = await pool.query('SELECT COUNT(*) as totalRegistrations FROM Registration');
    const [[{ confirmedRegistrations }]] = await pool.query("SELECT COUNT(*) as confirmedRegistrations FROM Registration WHERE Registration_Status = 'CONFIRMED'");
    const [[{ waitlistedRegistrations }]] = await pool.query("SELECT COUNT(*) as waitlistedRegistrations FROM Registration WHERE Registration_Status = 'WAITLISTED'");
    const [[{ totalRevenue }]] = await pool.query("SELECT IFNULL(SUM(Amount), 0) as totalRevenue FROM Transaction WHERE Transaction_Status = 'SUCCESS'");
    const [[{ userWalletSum }]] = await pool.query('SELECT IFNULL(SUM(Balance), 0) as userWalletSum FROM User');
    const [[{ instituteWalletSum }]] = await pool.query('SELECT IFNULL(SUM(Balance), 0) as instituteWalletSum FROM Institute');

    res.json({
      success: true,
      data: {
        totalEvents,
        openEvents,
        totalInstitutes,
        totalUsers,
        totalRegistrations,
        confirmedRegistrations,
        waitlistedRegistrations,
        totalRevenue: parseFloat(totalRevenue),
        userWalletSum: parseFloat(userWalletSum),
        instituteWalletSum: parseFloat(instituteWalletSum)
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// ============================================
// EVENTS APIS
// ============================================

app.get('/api/events', async (req, res) => {
  try {
    const { category, eligibility, status, search } = req.query;
    let query = `
      SELECT 
        e.*,
        i.Institute_Name,
        i.Email as Institute_Email,
        (SELECT COUNT(*) FROM Registration r WHERE r.Event_ID = e.Event_ID AND r.Registration_Status = 'CONFIRMED') AS ConfirmedCount,
        (SELECT COUNT(*) FROM Registration r WHERE r.Event_ID = e.Event_ID AND r.Registration_Status = 'WAITLISTED') AS WaitlistCount
      FROM Event e
      JOIN Institute i ON e.Institute_ID = i.Institute_ID
      WHERE 1=1
    `;
    const params = [];

    if (category && category !== 'ALL') {
      query += ' AND e.Category = ?';
      params.push(category);
    }
    if (eligibility && eligibility !== 'ALL') {
      query += ' AND (e.Eligibility = ? OR e.Eligibility = "ALL")';
      params.push(eligibility);
    }
    if (status && status !== 'ALL') {
      query += ' AND e.Status = ?';
      params.push(status);
    }
    if (search) {
      query += ' AND (e.Event_Name LIKE ? OR e.Description LIKE ? OR e.Venue LIKE ? OR i.Institute_Name LIKE ?)';
      const s = `%${search}%`;
      params.push(s, s, s, s);
    }

    query += ' ORDER BY e.Event_Date ASC, e.Start_Time ASC';

    const [events] = await pool.query(query, params);
    res.json({ success: true, count: events.length, data: events });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.get('/api/events/:id', async (req, res) => {
  try {
    const eventId = req.params.id;
    const query = `
      SELECT 
        e.*,
        i.Institute_Name,
        i.Email as Institute_Email,
        (SELECT COUNT(*) FROM Registration r WHERE r.Event_ID = e.Event_ID AND r.Registration_Status = 'CONFIRMED') AS ConfirmedCount,
        (SELECT COUNT(*) FROM Registration r WHERE r.Event_ID = e.Event_ID AND r.Registration_Status = 'WAITLISTED') AS WaitlistCount
      FROM Event e
      JOIN Institute i ON e.Institute_ID = i.Institute_ID
      WHERE e.Event_ID = ?
    `;
    const [rows] = await pool.query(query, [eventId]);
    if (rows.length === 0) return res.status(404).json({ success: false, error: 'Event not found' });

    const [registrations] = await pool.query(`
      SELECT r.*, u.Name as UserName, u.Email as UserEmail, u.Role as UserRole, u.Enrollment_No, t.Amount, t.Payment_Mode, t.Transaction_Status
      FROM Registration r
      JOIN User u ON r.User_ID = u.User_ID
      LEFT JOIN Transaction t ON r.Registration_ID = t.Registration_ID
      WHERE r.Event_ID = ?
      ORDER BY r.Registration_Date DESC
    `, [eventId]);

    res.json({ success: true, data: rows[0], registrations });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// CREATE EVENT - STRICT ROLE ENFORCEMENT: ONLY INSTITUTES CAN CREATE EVENTS
app.post('/api/events', async (req, res) => {
  try {
    const {
      callerRole,
      instituteId, eventName, category, eligibility, participationType,
      description, venue, capacity, ticketPrice, eventDate, startTime, endTime, registrationDeadline
    } = req.body;

    // Enforce business rule: Normal users (STUDENT, FACULTY, EXTERNAL) cannot host events
    if (callerRole !== 'INSTITUTE') {
      return res.status(403).json({
        success: false,
        error: 'Access Denied: Normal users (Students, Faculty, External) cannot host events. Only registered Institutes can create campus events.'
      });
    }

    if (!instituteId || !eventName || !category || !eligibility || !participationType || !capacity || !eventDate) {
      return res.status(400).json({ success: false, error: 'Missing required event fields.' });
    }

    // Verify institute exists
    const [[inst]] = await pool.query('SELECT Institute_ID FROM Institute WHERE Institute_ID = ?', [instituteId]);
    if (!inst) {
      return res.status(404).json({ success: false, error: 'Host Institute not found.' });
    }

    const [result] = await pool.query(`
      INSERT INTO Event (
        Institute_ID, Event_Name, Category, Eligibility, Participation_Type,
        Description, Venue, Capacity, Ticket_Price, Event_Date, Start_Time, End_Time, Registration_Deadline, Status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN')
    `, [
      instituteId, eventName, category, eligibility, participationType,
      description || '', venue || 'Campus Main Hall', capacity, ticketPrice || 0,
      eventDate, startTime || '09:00:00', endTime || '17:00:00', registrationDeadline || `${eventDate} 23:59:59`
    ]);

    res.json({
      success: true,
      message: 'Event created and published successfully by Institute.',
      eventId: result.insertId
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// CANCEL EVENT (Institute only - sp_cancel_event_by_institute with 100% refund)
app.post('/api/events/:id/cancel', async (req, res) => {
  try {
    const eventId = req.params.id;
    const { callerRole, instituteId } = req.body;

    if (callerRole !== 'INSTITUTE') {
      return res.status(403).json({ success: false, error: 'Only the hosting Institute can cancel this event.' });
    }

    await pool.query(
      'CALL sp_cancel_event_by_institute(?, @p_status, @p_message, @p_refunded_count)',
      [eventId]
    );
    const [[outParams]] = await pool.query('SELECT @p_status as status, @p_message as message, @p_refunded_count as refundedCount');

    if (outParams.status === 'SUCCESS') {
      res.json({ success: true, ...outParams });
    } else {
      res.status(400).json({ success: false, ...outParams });
    }
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// ============================================
// USERS & INSTITUTES DATA APIS
// ============================================

app.get('/api/users', async (req, res) => {
  try {
    const [users] = await pool.query(`
      SELECT u.User_ID, u.Name, u.Email, u.Phone, u.Role, u.Institute_ID, u.Enrollment_No, u.Employee_ID, u.Balance,
             i.Institute_Name
      FROM User u
      LEFT JOIN Institute i ON u.Institute_ID = i.Institute_ID
      ORDER BY u.User_ID ASC
    `);
    res.json({ success: true, data: users });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.get('/api/users/:id', async (req, res) => {
  try {
    const userId = req.params.id;
    const [[user]] = await pool.query(`
      SELECT u.User_ID, u.Name, u.Email, u.Phone, u.Role, u.Institute_ID, u.Enrollment_No, u.Employee_ID, u.Balance,
             i.Institute_Name
      FROM User u
      LEFT JOIN Institute i ON u.Institute_ID = i.Institute_ID
      WHERE u.User_ID = ?
    `, [userId]);

    if (!user) return res.status(404).json({ success: false, error: 'User not found' });

    const [registrations] = await pool.query(`
      SELECT r.*, e.Event_Name, e.Category, e.Event_Date, e.Venue, e.Ticket_Price, e.Status as EventStatus,
             i.Institute_Name, t.Transaction_ID, t.Amount as PaidAmount, t.Payment_Mode, t.Transaction_Status
      FROM Registration r
      JOIN Event e ON r.Event_ID = e.Event_ID
      JOIN Institute i ON e.Institute_ID = i.Institute_ID
      LEFT JOIN Transaction t ON r.Registration_ID = t.Registration_ID
      WHERE r.User_ID = ?
      ORDER BY r.Registration_Date DESC
    `, [userId]);

    const [walletTxns] = await pool.query(`
      SELECT * FROM Wallet_Transaction 
      WHERE Owner_Type = 'USER' AND Owner_ID = ? 
      ORDER BY Entry_Date DESC LIMIT 20
    `, [userId]);

    res.json({ success: true, data: user, registrations, walletTransactions: walletTxns });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.get('/api/institutes', async (req, res) => {
  try {
    const [institutes] = await pool.query(`
      SELECT i.Institute_ID, i.Institute_Name, i.Email, i.Address, i.Balance, i.Joining_Date,
             (SELECT COUNT(*) FROM Event e WHERE e.Institute_ID = i.Institute_ID) as EventCount
      FROM Institute i
      ORDER BY i.Institute_ID ASC
    `);
    res.json({ success: true, data: institutes });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.get('/api/institutes/:id/events', async (req, res) => {
  try {
    const instId = req.params.id;
    const [events] = await pool.query(`
      SELECT e.*,
        (SELECT COUNT(*) FROM Registration r WHERE r.Event_ID = e.Event_ID AND r.Registration_Status = 'CONFIRMED') AS ConfirmedCount,
        (SELECT COUNT(*) FROM Registration r WHERE r.Event_ID = e.Event_ID AND r.Registration_Status = 'WAITLISTED') AS WaitlistCount
      FROM Event e
      WHERE e.Institute_ID = ?
      ORDER BY e.Event_Date DESC
    `, [instId]);
    res.json({ success: true, data: events });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// ============================================
// REGISTRATION & WALLET APIS
// ============================================

app.get('/api/registrations', async (req, res) => {
  try {
    const [registrations] = await pool.query(`
      SELECT 
        r.Registration_ID, r.User_ID, r.Event_ID, r.Registration_Date, r.Registration_Status, r.Team_ID,
        u.Name as UserName, u.Email as UserEmail, u.Role as UserRole, u.Enrollment_No,
        e.Event_Name, e.Category, e.Event_Date, e.Ticket_Price,
        i.Institute_Name,
        t.Transaction_ID, t.Amount, t.Payment_Mode, t.Transaction_Status
      FROM Registration r
      JOIN User u ON r.User_ID = u.User_ID
      JOIN Event e ON r.Event_ID = e.Event_ID
      JOIN Institute i ON e.Institute_ID = i.Institute_ID
      LEFT JOIN Transaction t ON r.Registration_ID = t.Registration_ID
      ORDER BY r.Registration_Date DESC
    `);
    res.json({ success: true, data: registrations });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.post('/api/register', async (req, res) => {
  try {
    const { userId, eventId, paymentMode = 'WALLET', isTeam = false, teamMembers = [] } = req.body;

    if (!userId || !eventId) {
      return res.status(400).json({ success: false, error: 'User ID and Event ID are required. Please log in first.' });
    }

    const [[event]] = await pool.query('SELECT * FROM Event WHERE Event_ID = ?', [eventId]);
    if (!event) return res.status(404).json({ success: false, error: 'Event not found.' });

    if (event.Status !== 'OPEN') {
      return res.status(400).json({ success: false, error: `Cannot register. Event status is ${event.Status}.` });
    }

    const [[user]] = await pool.query('SELECT * FROM User WHERE User_ID = ?', [userId]);
    if (!user) return res.status(404).json({ success: false, error: 'User not found.' });

    // Check Eligibility
    if (event.Eligibility === 'STUDENT_NATIVE') {
      if (user.Role !== 'STUDENT' || user.Institute_ID !== event.Institute_ID) {
        return res.status(403).json({ success: false, error: 'Eligibility check failed: Only native students of the hosting institute can register.' });
      }
    } else if (event.Eligibility === 'FACULTY_NATIVE') {
      if (user.Role !== 'FACULTY' || user.Institute_ID !== event.Institute_ID) {
        return res.status(403).json({ success: false, error: 'Eligibility check failed: Only native faculty of the hosting institute can register.' });
      }
    } else if (event.Eligibility === 'EXTERNAL') {
      if (user.Role !== 'EXTERNAL') {
        return res.status(403).json({ success: false, error: 'Eligibility check failed: Only external participants can register.' });
      }
    }

    // Check Capacity
    const [[{ confirmedCount }]] = await pool.query(
      "SELECT COUNT(*) as confirmedCount FROM Registration WHERE Event_ID = ? AND Registration_Status = 'CONFIRMED'",
      [eventId]
    );

    if (confirmedCount >= event.Capacity) {
      const [waitlistResult] = await pool.query(
        "INSERT INTO Registration (User_ID, Event_ID, Registration_Status, Team_ID) VALUES (?, ?, 'WAITLISTED', NULL)",
        [userId, eventId]
      );
      return res.json({
        success: true,
        waitlisted: true,
        registrationId: waitlistResult.insertId,
        message: 'Event capacity is full. You have been placed on the FIFO Waiting List.'
      });
    }

    const ticketPrice = parseFloat(event.Ticket_Price);
    const totalAmount = ticketPrice;
    const isFree = totalAmount === 0;

    let registrationId, transactionId;

    if (isFree) {
      await pool.query(
        'CALL sp_register_free(?, ?, ?, ?, @p_reg_id, @p_txn_id, @p_status, @p_message)',
        [userId, eventId, event.Institute_ID, isTeam]
      );
      const [[outParams]] = await pool.query(
        'SELECT @p_reg_id as regId, @p_txn_id as txnId, @p_status as status, @p_message as message'
      );

      if (outParams.status !== 'SUCCESS') {
        return res.status(400).json({ success: false, error: outParams.message });
      }
      registrationId = outParams.regId;
      transactionId = outParams.txnId;
    } else {
      const isCash = paymentMode === 'CASH';
      const actualPaymentMode = isCash ? 'CASH' : 'UPI';
      await pool.query(
        'CALL sp_register_and_pay(?, ?, ?, ?, ?, ?, ?, @p_reg_id, @p_txn_id, @p_status, @p_message)',
        [userId, eventId, event.Institute_ID, totalAmount, actualPaymentMode, isTeam, isCash]
      );
      const [[outParams]] = await pool.query(
        'SELECT @p_reg_id as regId, @p_txn_id as txnId, @p_status as status, @p_message as message'
      );

      if (outParams.status !== 'SUCCESS') {
        return res.status(400).json({ success: false, error: outParams.message });
      }
      registrationId = outParams.regId;
      transactionId = outParams.txnId;
    }

    if (isTeam && Array.isArray(teamMembers) && teamMembers.length > 0 && registrationId) {
      for (const m of teamMembers) {
        if (m.name && m.branch && m.enrollmentNo) {
          await pool.query(
            'INSERT INTO Team_Member (Registration_ID, Name, Branch, Enrollment_No) VALUES (?, ?, ?, ?)',
            [registrationId, m.name, m.branch, m.enrollmentNo]
          );
        }
      }
    }

    res.json({
      success: true,
      registrationId,
      transactionId,
      message: isFree ? 'Successfully registered for free event!' : 'Successfully registered & paid via in-app wallet!'
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.post('/api/registrations/:id/cancel', async (req, res) => {
  try {
    const registrationId = req.params.id;
    await pool.query(
      'CALL sp_cancel_registration_by_user(?, @p_status, @p_message, @p_refund_amount)',
      [registrationId]
    );
    const [[outParams]] = await pool.query(
      'SELECT @p_status as status, @p_message as message, @p_refund_amount as refundAmount'
    );

    if (outParams.status === 'SUCCESS') {
      res.json({ success: true, ...outParams });
    } else {
      res.status(400).json({ success: false, ...outParams });
    }
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.post('/api/wallet/recharge', async (req, res) => {
  try {
    const { userId, amount } = req.body;
    if (!userId || !amount || parseFloat(amount) <= 0) {
      return res.status(400).json({ success: false, error: 'Valid User ID and positive amount required.' });
    }

    await pool.query('CALL sp_recharge_wallet(?, ?, @p_status, @p_message)', [userId, parseFloat(amount)]);
    const [[outParams]] = await pool.query('SELECT @p_status as status, @p_message as message');

    if (outParams.status === 'SUCCESS') {
      const [[user]] = await pool.query('SELECT Balance FROM User WHERE User_ID = ?', [userId]);
      res.json({ success: true, message: outParams.message, newBalance: user.Balance });
    } else {
      res.status(400).json({ success: false, error: outParams.message });
    }
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.get('/api/transactions', async (req, res) => {
  try {
    const [txns] = await pool.query(`
      SELECT t.*, u.Name as UserName, u.Email as UserEmail, i.Institute_Name, e.Event_Name
      FROM Transaction t
      JOIN User u ON t.User_ID = u.User_ID
      JOIN Institute i ON t.Institute_ID = i.Institute_ID
      JOIN Registration r ON t.Registration_ID = r.Registration_ID
      JOIN Event e ON r.Event_ID = e.Event_ID
      ORDER BY t.Transaction_Date DESC
    `);
    res.json({ success: true, data: txns });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.get('/api/wallet-transactions', async (req, res) => {
  try {
    const [ledger] = await pool.query(`
      SELECT wt.*,
        CASE 
          WHEN wt.Owner_Type = 'USER' THEN (SELECT Name FROM User WHERE User_ID = wt.Owner_ID)
          WHEN wt.Owner_Type = 'INSTITUTE' THEN (SELECT Institute_Name FROM Institute WHERE Institute_ID = wt.Owner_ID)
        END as OwnerName
      FROM Wallet_Transaction wt
      ORDER BY wt.Entry_Date DESC
      LIMIT 100
    `);
    res.json({ success: true, data: ledger });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.get('/api/schema-summary', async (req, res) => {
  try {
    const tables = ['Institute', 'User', 'Event', 'Registration', 'Team_Member', 'Transaction', 'Wallet_Transaction'];
    const summary = [];
    for (const t of tables) {
      const [[{ count }]] = await pool.query(`SELECT COUNT(*) as count FROM ${t}`);
      const [columns] = await pool.query(`SHOW COLUMNS FROM ${t}`);
      summary.push({
        table: t,
        rows: count,
        columnCount: columns.length,
        columns: columns.map(c => ({ field: c.Field, type: c.Type, null: c.Null, key: c.Key, default: c.Default }))
      });
    }
    res.json({ success: true, tables: summary });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`[EventNova Web] Server running on http://localhost:${PORT}`);
});
