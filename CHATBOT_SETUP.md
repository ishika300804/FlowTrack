# 🤖 Chatbot Integration

## ✅ What's Already Integrated
The chatbot is **fully integrated** into your IMS application! Here's what's been set up:

### Backend Components
- ✅ **ChatbotController** - REST API endpoint at `/api/chatbot/chat`
- ✅ **GeminiChatService** - AI service using Google Gemini API
- ✅ **ChatbotDatabaseService** - Database query execution
- ✅ **ChatbotToolsConfig** - Function declarations for AI tools
- ✅ **FunctionDeclaration** - Model for tool definitions

### Frontend Components
- ✅ **chatbot.css** - Beautiful floating widget styling
- ✅ **chatbot.js** - Interactive chat interface
- ✅ **Integrated in footer.html** - Available on all pages

### Available Chatbot Functions
1. **getAllInventoryItems** - Get all inventory items
2. **getAllVendors** - Get all vendors/suppliers
3. **getAllBorrowers** - Get all borrowers
4. **getAllLoans** - Get all loan records
5. **getItemById** - Get specific item details
6. **getLowStockItems** - Get items with low stock

---

## 🔧 Setup Instructions

### Step 1: Get Gemini API Key

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click **"Get API Key"** or **"Create API Key"**
4. Copy your API key

### Step 2: Configure API Key

**Option A: Environment Variable (Recommended)**
```bash
# Windows CMD
set GEMINI_API_KEY=your_actual_api_key_here

# Windows PowerShell
$env:GEMINI_API_KEY="your_actual_api_key_here"
```

**Option B: Direct in application.properties**
```properties
gemini.api.key=your_actual_api_key_here
```

### Step 3: Restart Application

```bash
# Stop current application (Ctrl+C)
# Then restart
start.bat
```

---

## 🎯 How to Use

### 1. Open Your Application
Navigate to: `http://localhost:8087`

### 2. Look for the Chat Button
You'll see a **purple floating button** in the bottom-right corner with a chat icon.

### 3. Click to Open Chat
The chatbot window will slide up from the bottom.

### 4. Ask Questions!

**Example Questions:**
- "Show me all inventory items"
- "What items are low on stock?"
- "List all vendors"
- "How many borrowers do we have?"
- "Show me item with ID 1"
- "Which items have less than 5 in stock?"
- "Show me all active loans"

---

## 🎨 Chatbot Features

### Visual Design
- **Floating Widget** - Non-intrusive purple button
- **Smooth Animations** - Slide-in/out transitions
- **Modern UI** - Gradient colors and rounded corners
- **Typing Indicator** - Shows when AI is thinking
- **Message Bubbles** - User (right) and Bot (left)
- **Avatars** - User and robot icons

### Functionality
- **Real-time Chat** - Instant responses
- **Database Integration** - Live data from your IMS
- **Context Awareness** - Understands inventory terminology
- **Error Handling** - Graceful error messages
- **Responsive** - Works on all screen sizes

---

## 🧪 Testing the Chatbot

### Test 1: Basic Connectivity
```
User: "Hello"
Expected: Greeting response
```

### Test 2: Get All Items
```
User: "Show me all inventory items"
Expected: List of items with details
```

### Test 3: Low Stock Alert
```
User: "What items are running low?"
Expected: Items below threshold (default: 10)
```

### Test 4: Specific Item
```
User: "Show me details for item 1"
Expected: Specific item information
```

### Test 5: Vendor Information
```
User: "List all vendors"
Expected: All vendor records
```

---

## 🔍 Troubleshooting

### Chatbot Button Not Showing
- ✅ Check browser console for errors (F12)
- ✅ Verify `/js/chatbot.js` is loading
- ✅ Clear browser cache (Ctrl+Shift+R)

### "Error" Response from Bot
- ✅ Check Gemini API key is set correctly
- ✅ Verify internet connection
- ✅ Check application logs for errors
- ✅ Ensure API key has proper permissions

### No Response from Bot
- ✅ Check `/api/chatbot/test` endpoint: `http://localhost:8087/api/chatbot/test`
- ✅ Verify backend is running
- ✅ Check browser network tab (F12 → Network)

### Database Queries Not Working
- ✅ Verify database connection
- ✅ Check sample data is loaded
- ✅ Review application logs

---

## 📝 API Endpoints

### Chat Endpoint
```
POST /api/chatbot/chat
Content-Type: application/json

{
  "message": "Show me all items"
}

Response:
{
  "response": "Here are all the inventory items..."
}
```

### Test Endpoint
```
GET /api/chatbot/test

Response: "✅ Chatbot API is working!"
```

---

## 🎨 Customization

### Change Chatbot Colors
Edit `src/main/resources/static/css/chatbot.css`:
```css
/* Change gradient colors */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

### Modify Welcome Message
Edit `src/main/resources/static/js/chatbot.js`:
```javascript
addWelcomeMessage() {
    this.addMessage('bot', 'Your custom welcome message here!');
}
```

### Add More Functions
Edit `src/main/java/com/example/IMS/chatbot/config/ChatbotToolsConfig.java`:
```java
tools.add(new FunctionDeclaration(
    "yourFunctionName",
    "Description of what it does",
    Map.of(/* parameters */)
));
```

---

## 🚀 Next Steps

1. **Get your Gemini API key** from Google AI Studio
2. **Set the environment variable** or update application.properties
3. **Restart the application**
4. **Test the chatbot** with sample questions
5. **Customize** colors and messages to match your brand

---

## 📚 Resources

- [Google Gemini API Documentation](https://ai.google.dev/docs)
- [Gemini API Key Setup](https://makersuite.google.com/app/apikey)
- [Function Calling Guide](https://ai.google.dev/docs/function_calling)

---

## ✨ Features Summary

✅ **Fully Integrated** - Works out of the box
✅ **AI-Powered** - Uses Google Gemini 2.0
✅ **Database Connected** - Real-time IMS data
✅ **Beautiful UI** - Modern, responsive design
✅ **Easy to Use** - Natural language queries
✅ **Extensible** - Add more functions easily

**The chatbot is ready to use! Just add your API key and restart the application.**
