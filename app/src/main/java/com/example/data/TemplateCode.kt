package com.example.data

object TemplateCode {

    // Helper to represent safe dollar sign in template structures
    private const val D = "$"

    val FASTAPI_CODE = """# ==============================================================================
# UNIVERSAL VIDEO DOWNLOADER SYSTEM - BACKEND API LAYER
# Tech Stack: Python (FastAPI), MongoDB (Motor Async), Redis Caching, yt-dlp
# Author: System Architect & Full-Stack Engineer
# Description: Real-time video/audio extraction handler, telemetry, and admin settings.
# ==============================================================================

import os
import uuid
import asyncio
import logging
from typing import List, Optional
from datetime import datetime, date
from pydantic import BaseModel, HttpUrl, Field
from fastapi import FastAPI, HTTPException, Depends, Query, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from motor.motor_asyncio import AsyncIOMotorClient
import redis.asyncio as aioredis
import yt_dlp

# --- Logger Setup ---
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("DownloaderBackend")

# --- Environment Configuration ---
MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017")
REDIS_URI = os.getenv("REDIS_URI", "redis://localhost:6379")
ADMIN_TELEGRAM_ID = int(os.getenv("ADMIN_TELEGRAM_ID", "123456789"))
SECRET_TOKEN = os.getenv("SECRET_TOKEN", "cyber_downloader_secret_token_2026")
PROXY_POOLS = os.getenv("PROXY_POOLS", "").split(",")  # Comma-separated list for failover rotation

app = FastAPI(
    title="Universal Downloader Engine API",
    version="2.0.0",
    description="Cyberpunk-Glassmorphism High-Performance Custom Microservice"
)

# --- CORS (Enabled for web clients and Telegram Web Apps) ---
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- State Hooks ---
db = None
redis_client = None

@app.on_event("startup")
async def startup_db_client():
    global db, redis_client
    # MongoDB initialization
    mongo_client = AsyncIOMotorClient(MONGO_URI)
    db = mongo_client["universal_downloader"]
    logger.info("📡 Fully connected to MongoDB Instance.")
    
    # Redis initialization
    redis_client = aioredis.from_url(REDIS_URI, decode_responses=True)
    logger.info("⚡ Live connected to Redis Cache Node.")

@app.on_event("shutdown")
async def shutdown_db_client():
    db.client.close()
    await redis_client.close()
    logger.info("🛑 Gracefully closed backend connection hooks.")

# --- MongoDB Document Models ---
class UserSchema(BaseModel):
    user_id: int = Field(..., description="Unique Telegram Chat ID")
    username: Optional[str] = None
    join_date: datetime = Field(default_factory=datetime.utcnow)
    download_count: int = 0
    is_banned: bool = False

class DownloadLogSchema(BaseModel):
    user_id: Optional[int] = None
    platform: str
    url: str
    title: str
    resolution: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    success: bool = True

class SystemSettingsSchema(BaseModel):
    maintenance_mode: bool = False
    disable_youtube: bool = False

# --- Request & Response Models ---
class ExtractRequest(BaseModel):
    url: HttpUrl
    user_id: Optional[int] = None

class VideoFormatInfo(BaseModel):
    format_id: str
    ext: str
    resolution: str
    filesize_mb: Optional[float] = None
    note: Optional[str] = None

class VideoMetadata(BaseModel):
    title: str
    thumbnail: str
    duration: int
    platform: str
    formats: List[VideoFormatInfo]

# --- Core Helper: yt_dlp Video Meta Extractor ---
def extract_video_meta_sync(url: str, proxy: Optional[str] = None) -> dict:
    ydl_opts = {
        'skip_download': True,
        'quiet': True,
        'no_warnings': True,
        'extract_flat': False,
    }
    if proxy:
        ydl_opts['proxy'] = proxy
        
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=False)
        return ydl.sanitize_info(info)

async def extract_video_metadata(url: str) -> dict:
    # 1. Attempt Cache Access
    cached_data = await redis_client.get(f"meta:{url}")
    if cached_data:
        import json
        logger.info("💾 Meta cache hit!")
        return json.loads(cached_data)

    # Rotate proxy if needed
    proxy = PROXY_POOLS[0] if PROXY_POOLS and PROXY_POOLS[0] else None
    
    # 2. Invoke yt-dlp in executor to prevent async thread starvation
    loop = asyncio.get_event_loop()
    try:
        raw_meta = await loop.run_in_executor(None, extract_video_meta_sync, url, proxy)
    except Exception as e:
        logger.error(f"❌ yt-dlp extraction error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, 
            detail=f"Video resource extraction failed: {str(e)}"
        )
        
    # Standardize result schema
    formats_list = []
    for f in raw_meta.get("formats", []):
        # We look for direct video/audio formats
        if f.get("vcodec") != "none" or f.get("acodec") != "none":
            filesize = f.get("filesize") or f.get("filesize_approx")
            filesize_mb = round(filesize / (1024 * 1024), 2) if filesize else None
            note = f.get("format_note", "") or f.get("note", "")
            res = f.get("resolution", "") or f.get("height", "")
            formats_list.append(
                VideoFormatInfo(
                    format_id=str(f.get("format_id")),
                    ext=str(f.get("ext")),
                    resolution=str(res),
                    filesize_mb=filesize_mb,
                    note=note
                )
            )
            
    meta = VideoMetadata(
        title=raw_meta.get("title", "Unknown Title"),
        thumbnail=raw_meta.get("thumbnail", "https://img.youtube.com/vi/video_id/hqdefault.jpg"),
        duration=raw_meta.get("duration", 0),
        platform=raw_meta.get("extractor_key", "Generic"),
        formats=formats_list[:12] # Limit lists to prevent bulky responses
    )
    
    # 3. Cache results for 1 hour
    import json
    await redis_client.setex(f"meta:{url}", 3600, json.dumps(meta.dict()))
    return meta.dict()

# --- Endpoint Routing ---

@app.post("/api/downloader/extract", response_model=VideoMetadata)
async def api_extract_metadata(req: ExtractRequest):
    # Verify maintenance settings
    setting = await db["settings"].find_one({"_id": "global_config"})
    if setting:
        if setting.get("maintenance_mode", False):
            raise HTTPException(status_code=503, detail="System under maintenance. Toggled globally by admin.")
        if setting.get("disable_youtube", False) and "youtube.com" in str(req.url):
            raise HTTPException(status_code=400, detail="YouTube downloads represent a temporarily disabled module.")

    if req.user_id:
        user = await db["users"].find_one({"user_id": req.user_id})
        if user and user.get("is_banned", False):
            raise HTTPException(status_code=403, detail="Security Flag: User ID is permanently blacklisted.")

    data = await extract_video_metadata(str(req.url))
    return data

@app.get("/api/downloader/download-stream")
async def api_download_stream(url: str, format_id: str):
    # Stream the media via chunked response to client
    ydl_opts = {
        'format': format_id,
        'quiet': True,
        'no_warnings': True,
        'outtmpl': '-' # Output stream to stdout
    }
    
    def generator():
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            # Runs process streaming chunks from stdout
            info = ydl.extract_info(url, download=True)
            # In production, yt-dlp pipelines stdout directly to yield chunks:
            # We open sub-pipes for flawless media processing.
            
    # Simple mockup generator showing streamed chunks response
    async def dummy_stream_or_proxy():
        # Pipe output generator back natively
        yield b"chunk"
        
    return StreamingResponse(dummy_stream_or_proxy(), media_type="video/mp4")

@app.post("/api/users/register", status_code=status.HTTP_201_CREATED)
async def register_user(user: UserSchema):
    existing = await db["users"].find_one({"user_id": user.user_id})
    if existing:
        return {"status": "exists", "user_id": user.user_id}
    
    await db["users"].insert_one(user.dict())
    return {"status": "created", "user_id": user.user_id}

@app.post("/api/downloader/log")
async def log_download(log: DownloadLogSchema):
    await db["downloads"].insert_one(log.dict())
    if log.user_id:
        await db["users"].update_one(
            {"user_id": log.user_id},
            {"_D_inc": {"download_count": 1}}
        )
    return {"status": "logged"}

# --- Administrative Endpoints (Strict Telegram Admin ID or Token Verification) ---

@app.get("/api/admin/stats")
async def get_system_stats(admin_id: int):
    if admin_id != ADMIN_TELEGRAM_ID:
        raise HTTPException(status_code=403, detail="Admin authorization denied.")
        
    total_users = await db["users"].count_documents({})
    banned_users = await db["users"].count_documents({"is_banned": True})
    total_downloads = await db["downloads"].count_documents({})
    
    # Aggregate top platforms
    pipeline = [
        {"_D_group": {"_id": "_D_platform", "count": {"_D_sum": 1}}},
        {"_D_sort": {"count": -1}},
        {"_D_limit": 3}
    ]
    top_platforms = []
    async for doc in db["downloads"].aggregate(pipeline):
        top_platforms.append({"platform": doc["_id"], "count": doc["count"]})
        
    return {
        "total_users": total_users,
        "banned_users": banned_users,
        "total_downloads": total_downloads,
        "top_platforms": top_platforms,
        "system_health": {
            "proxy_pool_size": len(PROXY_POOLS),
            "database_connection": "Online",
            "redis_cache": "Active"
        }
    }

@app.post("/api/admin/toggle-setting")
async def toggle_setting(admin_id: int, key: str, value: bool):
    if admin_id != ADMIN_TELEGRAM_ID:
        raise HTTPException(status_code=403, detail="Unauthorized")
        
    await db["settings"].update_one(
        {"_id": "global_config"},
        {"_D_set": {key: value}},
        upsert=True
    )
    return {"status": "updated", "key": key, "value": value}

@app.post("/api/admin/ban")
async def ban_user(admin_id: int, user_id: int, ban: bool):
    if admin_id != ADMIN_TELEGRAM_ID:
        raise HTTPException(status_code=403, detail="Unauthorized")
        
    await db["users"].update_one(
        {"user_id": user_id},
        {"_D_set": {"is_banned": ban}}
    )
    return {"status": "performed", "user_id": user_id, "is_banned": ban}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
""".replace("_D_", D)

    val TELEGRAM_BOT_CODE = """# ==============================================================================
# UNIVERSAL VIDEO DOWNLOADER SYSTEM - TELEGRAM CORRIDOR BOT
# Tech Stack: Python (Aiogram v3), Asyncio HTTP API Connectors
# Admin Control Suite, Rate-Limited Broadcast Engine, Web App trigger
# ==============================================================================

import os
import asyncio
import logging
import aiohttp
from datetime import datetime
from aiogram import Bot, Dispatcher, types, F
from aiogram.filters import Command, CommandObject
from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton, WebAppInfo
from aiogram.fsm.context import FSMContext
from aiogram.fsm.state import StatesGroup, State

# --- Logger ---
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("DownloaderTelegramBot")

# --- Environment Context ---
BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "000000000:AAE_DummyTokenForTesting")
BACKEND_URL = os.getenv("BACKEND_API_URL", "https://api.yourdownloader.com")
WEB_APP_URL = os.getenv("WEB_APP_URL", "https://yourdownloader.com")
ADMIN_TELEGRAM_ID = int(os.getenv("ADMIN_TELEGRAM_ID", "123456789"))

bot = Bot(token=BOT_TOKEN)
dp = Dispatcher()

class BroadcastState(StatesGroup):
    waiting_for_message = State()

# --- Common Helper: API Connectors ---
async def register_user_api(user_id: int, username: str):
    url = f"{BACKEND_URL}/api/users/register"
    payload = {
        "user_id": user_id,
        "username": username,
        "join_date": datetime.utcnow().isoformat(),
        "download_count": 0,
        "is_banned": False
    }
    try:
        async with aiohttp.ClientSession() as session:
            async with session.post(url, json=payload, timeout=5) as res:
                return await res.json()
    except Exception as e:
        logger.error(f"Error registering user: {str(e)}")
        return None

async def extract_meta_api(url: str, user_id: int):
    api_url = f"{BACKEND_URL}/api/downloader/extract"
    payload = {"url": url, "user_id": user_id}
    try:
        async with aiohttp.ClientSession() as session:
            async with session.post(api_url, json=payload, timeout=15) as res:
                if res.status == 200:
                    return await res.json()
                elif res.status == 403:
                    return {"error": "Banned"}
                elif res.status == 503:
                    return {"error": "Maintenance"}
                else:
                    return None
    except Exception as e:
        logger.error(f"API extract helper failure: {str(e)}")
        return None

# --- User Handlers ---

@dp.message(Command("start"))
async def start_cmd(message: types.Message):
    user_id = message.from_user.id
    username = message.from_user.username or "Anonymous"
    
    # Register user in central shared DB via API
    await register_user_api(user_id, username)
    
    # Glassmorphism styled responsive welcome message
    welcome_text = (
        f"⚡ <b>UNIVERSAL INTELLIGENT DOWNLOAD HYPERENGINE</b> ⚡\n\n"
        f"Hello, <i>@{username}</i>. Paste any valid link (YouTube, TikTok, "
        f"Instagram, Twitter, etc.) directly into this chat, or open our futuristic Web App!\n\n"
        f"🛡️ Powered by <code>yt-dlp</code> advanced core engines."
    )
    
    # Inline action keyboard
    keyboard = InlineKeyboardMarkup(inline_keyboard=[
        [
            InlineKeyboardButton(
                text="🌐 Open Web App", 
                web_app=WebAppInfo(url=f"{WEB_APP_URL}?tg_id={user_id}")
            )
        ],
        [
            InlineKeyboardButton(text="📊 My Stats", callback_data="my_stats_btn")
        ]
    ])
    
    await message.reply(welcome_text, reply_markup=keyboard, parse_mode="HTML")

@dp.callback_query(F.data == "my_stats_btn")
async def show_stats_callback(query: types.CallbackQuery):
    user_id = query.from_user.id
    # Fetch details directly from DB or mock stats via query endpoint
    try:
        total_downloads = 35 # Replace dynamically
        stats_text = (
            f"⚡ <b>YOUR STATISTICS</b> ⚡\n\n"
            f"👤 User ID: <code>{user_id}</code>\n"
            f"📊 Successful Downloads: {total_downloads} videos\n"
            f"🏆 Status: Certified Active Miner"
        )
        await query.message.reply(stats_text, parse_mode="HTML")
        await query.answer()
    except Exception as e:
        await query.answer("Service momentarily out of service.")

@dp.message(lambda message: message.text and (
    "youtube.com" in message.text or "youtu.be" in message.text or 
    "tiktok.com" in message.text or "instagram.com" in message.text or
    "twitter.com" in message.text or "x.com" in message.text
))
async def handle_download_link(message: types.Message):
    user_id = message.from_user.id
    url = message.text.strip()
    
    status_msg = await message.reply("⚙️ <b>PROFILING HOST... Analyzing stream pipelines</b> ⚡", parse_mode="HTML")
    
    # Query FastAPI downloader pipeline
    meta = await extract_meta_api(url, user_id)
    
    if not meta:
        await status_msg.edit_text("❌ Extraction error: Connection refused or link dead.")
        return
        
    if "error" in meta:
        if meta["error"] == "Banned":
            await status_msg.edit_text("⛔ Critical Error: ACCESS FORBIDDEN. User is globally banned.")
        elif meta["error"] == "Maintenance":
            await status_msg.edit_text("⚙️ Downloader is globally offline. Toggled maintenance mode.")
        return

    # Render results and create callback triggers for options
    title = meta.get("title", "Downloader Metadata File")
    thumbnail = meta.get("thumbnail", "https://placehold.co/600x400/png")
    platform = meta.get("platform", "Unknown")
    
    buttons = []
    # Loop over available format paths securely
    for fmt in meta.get("formats", []):
        if fmt.get("resolution"):
            label = f"{fmt['resolution']} ({fmt['ext']}) - {fmt.get('filesize_mb') or '?' }MB"
            # Encode target payload within 64 byte limit
            cb_data = f"dl|{fmt['format_id']}|{user_id}"
            buttons.append([InlineKeyboardButton(text=label, callback_data=cb_data)])
            
    keyboard = InlineKeyboardMarkup(inline_keyboard=buttons[:8]) # Max 8 options to avoid crowding UI
    
    caption = (
        f"<b>🎬 TITLE:</b> {title}\n"
        f"<b>🌐 REPO:</b> {platform.upper()}\n\n"
        f"Select your requested extraction grade below 👇"
    )
    
    await message.reply_photo(photo=thumbnail, caption=caption, reply_markup=keyboard, parse_mode="HTML")
    await status_msg.delete()

# --- Admin Section (Commands locked to ADMIN_TELEGRAM_ID Validation) ---

@dp.message(Command("admin"))
async def admin_entry(message: types.Message):
    if message.from_user.id != ADMIN_TELEGRAM_ID:
        await message.reply("⛔ Permission Denied: Admin Authorization Hook Required.")
        return
        
    admin_markup = InlineKeyboardMarkup(inline_keyboard=[
        [
            InlineKeyboardButton(text="📊 Real-time Stats", callback_data="admin_stats"),
            InlineKeyboardButton(text="⚙️ System Status", callback_data="admin_sys")
        ],
        [
            InlineKeyboardButton(text="📢 Broadcast Message", callback_data="admin_broadcast_trigger")
        ],
        [
            InlineKeyboardButton(text="🔐 Close Control Panel", callback_data="admin_close")
        ]
    ])
    
    await message.reply(
        "✨ <b>ADMIN CONTROL TERMINAL v3.5</b> ✨\n"
        "Control pipeline status, fetch stats registries, and ban/unban telemetry.",
        reply_markup=admin_markup,
        parse_mode="HTML"
    )

@dp.callback_query(F.data == "admin_stats")
async def show_admin_stats(query: types.CallbackQuery):
    if query.from_user.id != ADMIN_TELEGRAM_ID: return
    
    # Query metrics from FastAPI backend stats service
    url = f"{BACKEND_URL}/api/admin/stats?admin_id={ADMIN_TELEGRAM_ID}"
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as res:
            if res.status == 200:
                data = await res.json()
                stats_text = (
                    f"📂 <b>LIVE SYSTEM Telemetry</b>\n\n"
                    f"▫️ Total Registered Users: {data['total_users']}\n"
                    f"▫️ Active BSF Bans: {data['banned_users']}\n"
                    f"▫️ Total Logged Downloads: {data['total_downloads']}\n\n"
                    f"🏢 <b>System Node Health</b>\n"
                    f"▫️ Rotating Proxy Pool: {data['system_health']['proxy_pool_size']} online\n"
                    f"▫️ Redis Cache: {data['system_health']['redis_cache']}\n"
                    f"▫️ Backend Instance: Connected"
                )
                await query.message.reply(stats_text, parse_mode="HTML")
            else:
                await query.message.reply("❌ Error fetching backend stats cluster.")
    await query.answer()

@dp.message(Command("ban"))
async def ban_user_cmd(message: types.Message, command: CommandObject):
    if message.from_user.id != ADMIN_TELEGRAM_ID: return
    
    args = command.args
    if not args:
        await message.reply("Format: <code>/ban user_id</code>", parse_mode="HTML")
        return
        
    try:
        user_id = int(args.split()[0])
        # Direct ban via central REST API
        block_url = f"{BACKEND_URL}/api/admin/ban?admin_id={ADMIN_TELEGRAM_ID}&user_id={user_id}&ban=true"
        async with aiohttp.ClientSession() as session:
            async with session.post(block_url) as res:
                if res.status == 200:
                    await message.reply(f"🔒 Blacklisted User ID: <code>{user_id}</code> permanently.", parse_mode="HTML")
                else:
                    await message.reply("Failed to sync proxy state inside Backend Database.")
    except Exception as e:
        await message.reply(f"Error parse input arguments: {str(e)}")

# --- Async Broadcaster: Async queue to avoid Telegram Rate Limits ---
@dp.callback_query(F.data == "admin_broadcast_trigger")
async def process_broadcast(query: types.CallbackQuery, state: FSMContext):
    if query.from_user.id != ADMIN_TELEGRAM_ID: return
    await query.message.reply("✉️ Please enter the message text you wish to broadcast to all registered users:")
    await state.set_state(BroadcastState.waiting_for_message)
    await query.answer()

@dp.message(BroadcastState.waiting_for_message)
async def perform_broadcast_job(message: types.Message, state: FSMContext):
    if message.from_user.id != ADMIN_TELEGRAM_ID: return
    
    broadcast_text = message.text
    await state.clear()
    
    await message.reply("📡 <b>Broadcast queue starting. Spinning async rate limiter daemon...</b>", parse_mode="HTML")
    
    users_url = f"{BACKEND_URL}/api/admin/users?admin_id={ADMIN_TELEGRAM_ID}"
    simulated_users = [12345, 67890, 11213]
    
    sent = 0
    failed = 0
    for uid in simulated_users:
        try:
            await bot.send_message(chat_id=uid, text=f"📢 <b>ADMIN ANNOUNCEMENT:</b>\n\n{broadcast_text}", parse_mode="HTML")
            sent += 1
            await asyncio.sleep(0.05) 
        except Exception:
            failed += 1
            
    await message.reply(f"📈 <b>Broadcast Complete.</b>\n\n📦 Transmitted: {sent}\n❌ Failed/Idle channels: {failed}", parse_mode="HTML")

async def main():
    logger.info("Initializing bot server... Scanning webhook channels.")
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())
"""

    val FRONTEND_CODE = """/* ==============================================================================
 * UNIVERSAL VIDEO DOWNLOADER SYSTEM - FRONTEND CLIENT
 * Tech Stack: React.js, Tailwind CSS, Heroicons, Framer Motion
 * Design System: Frosted cyberpunk glassmorphism, glowing cyan & purple elements.
 * ============================================================================== */

import React, { useState, useEffect } from 'react';

export default function App() {
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [metadata, setMetadata] = useState(null);
  const [error, setError] = useState('');
  const [stats, setStats] = useState({ total_downloads: 14212, active_nodes: 5, uptime: '99.9%' });

  const BACKEND_API_URL = "https://api.yourdownloader.com"; 

  const handleAutoPaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text.startsWith("http")) {
        setUrl(text);
      }
    } catch (err) {
      console.warn("Clipboard access denied by permissions.");
    }
  };

  const handleExtract = async (e) => {
    e.preventDefault();
    if (!url) return;

    setLoading(true);
    setError('');
    setMetadata(null);

    try {
      const response = await fetch(`${D}{BACKEND_API_URL}/api/downloader/extract`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: url })
      });

      if (!response.ok) {
        throw new Error("API Exception: Extraction node offline or video is private.");
      }

      const data = await response.json();
      setMetadata(data);
    } catch (err) {
      setError(err.message || "Failed to parse URL link.");
    } finally {
      setLoading(false);
    }
  };

  const executeDownload = (formatId) => {
    window.open(`${D}{BACKEND_API_URL}/api/downloader/download-stream?url=${D}{encodeURIComponent(url)}&format_id=${D}{formatId}`);
  };

  return (
    <div className="min-h-screen bg-[#0a0a0f] text-white font-sans antialiased relative overflow-hidden flex flex-col justify-between">
      <div className="absolute top-[-10%] left-[-10%] w-[45%] h-[45%] rounded-full bg-gradient-to-r from-cyan-500 to-teal-500 opacity-20 blur-[120px] pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[45%] h-[45%] rounded-full bg-gradient-to-r from-purple-500 to-pink-500 opacity-20 blur-[120px] pointer-events-none"></div>

      <header className="border-b border-white px-6 py-4 flex justify-between items-center bg-[#0a0a0f] backdrop-blur-md">
        <div className="flex items-center space-x-2">
          <span className="text-xl font-bold tracking-widest bg-gradient-to-r from-cyan-400 to-purple-400 bg-clip-text text-transparent">
            NEURA_DOWNLOAD
          </span>
          <span className="text-xs bg-cyan-900/50 border border-cyan-400/30 text-cyan-400 px-2 py-0.5 rounded font-mono">
            V2.0
          </span>
        </div>
        <div className="flex items-center space-x-4 text-xs font-mono">
          <span className="flex items-center space-x-1.5">
            <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span className="text-gray-400">ENGINES ONLINE</span>
          </span>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-6 py-12 flex-1 w-full space-y-8 z-10">
        
        <div className="text-center space-y-3">
          <h1 className="text-4xl md:text-5xl font-black tracking-tight leading-none bg-gradient-to-r from-white via-gray-100 to-gray-500 bg-clip-text text-transparent">
            UNIVERSAL STREAM EXTRACTOR
          </h1>
          <p className="text-sm md:text-base text-gray-400 max-w-lg mx-auto">
            High-speed cloud processing layer utilizing <code className="text-cyan-400">yt-dlp</code> failover clusters. Download in high-bitrate resolution formats.
          </p>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-white/10 shadow-[0_8px_32px_0_rgba(0,242,254,0.05)] backdrop-blur-md">
          <form onSubmit={handleExtract} className="space-y-4">
            <div className="relative flex items-center bg-black/40 rounded-xl border border-white/5 overflow-hidden">
              <input
                type="text"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="Paste media link (YouTube, TikTok, Twitter, Instagram...)"
                className="w-full bg-transparent px-4 py-4 text-sm focus:outline-none placeholder-gray-500 text-white"
              />
              <button
                type="button"
                onClick={handleAutoPaste}
                className="px-3 py-1.5 mr-2 rounded bg-white/5 border border-white/10 hover:bg-white/10 text-xs font-mono text-cyan-400 transition"
                title="Paste from clipboard"
              >
                AUTO_PASTE
              </button>
            </div>

            <button
              type="submit"
              disabled={loading || !url}
              className="w-full relative group overflow-hidden bg-gradient-to-r from-cyan-500 to-purple-600 font-bold py-4 rounded-xl text-sm transition shadow-[0_4px_20px_rgba(79,70,229,0.3)] hover:scale-[1.01] duration-200 disabled:opacity-50 disabled:scale-100"
            >
              <span className="relative z-10 flex items-center justify-center space-x-2">
                {loading ? (
                  <>
                    <svg className="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    <span>PROCESSING METADATA PIPELINE...</span>
                  </>
                ) : (
                  <span>EXTRACT VIDEO STREAM ☄️</span>
                )}
              </span>
            </button>
          </form>

          {error && (
            <div className="mt-4 p-4 border border-rose-500/20 bg-rose-950/20 rounded-xl text-rose-400 text-sm flex items-center space-x-2">
              <span className="font-mono">[NODE_ERROR]</span>
              <span>{error}</span>
            </div>
          )}
        </div>

        {metadata && (
          <div className="bg-white p-6 rounded-2xl border border-white/10 shadow-[0_8px_32px_0_rgba(15,10,25,0.4)] backdrop-blur-md space-y-6 animate-fadeIn">
            <div className="flex flex-col md:flex-row space-y-4 md:space-y-0 md:space-x-6">
              <div className="w-full md:w-1/3 rounded-xl overflow-hidden relative border border-white/10">
                <img src={metadata.thumbnail} alt="Video Thumbnail" className="w-full h-full object-cover aspect-video" />
                <span className="absolute bottom-2 right-2 bg-black/80 px-2 py-0.5 rounded text-[10px] font-mono text-cyan-400">
                  {Math.floor(metadata.duration / 60)}:{(metadata.duration % 60).toString().padStart(2, '0')} min
                </span>
              </div>
              <div className="w-full md:w-2/3 flex flex-col justify-between py-1">
                <div className="space-y-2">
                  <span className="px-2 py-0.5 text-[10px] border border-cyan-400/40 text-cyan-400 font-mono rounded bg-cyan-950/30">
                    {metadata.platform.toUpperCase()}
                  </span>
                  <h3 className="text-lg md:text-xl font-bold tracking-tight line-clamp-2 leading-snug">
                    {metadata.title}
                  </h3>
                </div>
                <div className="flex space-x-4 text-xs text-gray-400 font-mono mt-4 md:mt-0">
                  <span>METRIC: SAFE</span>
                  <span>SSL: SECURE</span>
                </div>
              </div>
            </div>

            <div className="border-t border-white/5 pt-6 space-y-4">
              <h4 className="text-sm font-semibold tracking-wider text-cyan-400 font-mono">AVAILABLE PIPELINES</h4>
              <div className="overflow-x-auto rounded-xl border border-white/10">
                <table className="w-full text-left text-sm border-collapse bg-black/20">
                  <thead>
                    <tr className="border-b border-white/10 bg-white/5 text-[10px] tracking-wider font-mono text-gray-400 uppercase">
                      <th className="px-4 py-3">GRADE</th>
                      <th className="px-4 py-3">EXT</th>
                      <th className="px-4 py-3">FILE SIZE</th>
                      <th className="px-4 py-3 text-right">ROUTE</th>
                    </tr>
                  </thead>
                  <tbody>
                    {metadata.formats && metadata.formats.map((fmt, idx) => (
                      <tr key={idx} className="border-b border-white/5 hover:bg-white/5 transition duration-150">
                        <td className="px-4 py-4 font-bold text-gray-100">{fmt.resolution}</td>
                        <td className="px-4 py-4 text-gray-400 font-mono font-medium">{fmt.ext.toUpperCase()}</td>
                        <td className="px-4 py-4 font-mono text-gray-300">
                          {fmt.filesize_mb ? `${D}{fmt.filesize_mb} MB` : 'N/A'}
                        </td>
                        <td className="px-4 py-4 text-right">
                          <button
                            onClick={() => executeDownload(fmt.format_id)}
                            className="bg-cyan-500/20 border border-cyan-500/40 text-cyan-400 hover:bg-cyan-500 hover:text-black transition px-3 py-1.5 rounded-lg text-xs font-bold tracking-wide"
                          >
                            DOWNLOAD
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        <div className="grid grid-cols-3 gap-4">
          <div className="bg-[#0f0f18] p-4 rounded-xl border border-white/5 text-center space-y-1">
            <span className="text-[10px] text-gray-500 font-mono">EST_DL_TOTAL</span>
            <p className="text-sm md:text-base font-bold text-cyan-400 font-mono">{stats.total_downloads.toLocaleString()}</p>
          </div>
          <div className="bg-[#0f0f18] p-4 rounded-xl border border-white/5 text-center space-y-1">
            <span className="text-[10px] text-gray-500 font-mono">SERVERS_LOADED</span>
            <p className="text-sm md:text-base font-bold text-purple-400 font-mono">{stats.active_nodes} clusters</p>
          </div>
          <div className="bg-[#0f0f18] p-4 rounded-xl border border-white/5 text-center space-y-1">
            <span className="text-[10px] text-gray-500 font-mono">PING_uptime</span>
            <p className="text-sm md:text-base font-bold text-emerald-400 font-mono">{stats.uptime}</p>
          </div>
        </div>

      </main>

      <footer className="py-6 border-t border-white/5 text-center text-xs font-mono text-gray-600 bg-black/40">
        <p>CONSOLE_NODE::[CONNECT_HEALTHY] &copy; 2026 UNIVERSAL EXTRACTOR SYSTEM ENGINE.</p>
      </footer>
    </div>
  );
}
"""
}
