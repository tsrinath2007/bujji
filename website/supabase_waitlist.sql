-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/_/sql

CREATE TABLE IF NOT EXISTS public.waitlist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    full_name TEXT,
    avatar_url TEXT,
    provider TEXT DEFAULT 'email',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable Row Level Security (RLS)
ALTER TABLE public.waitlist ENABLE ROW LEVEL SECURITY;

-- Allow anyone (public anon) to insert their email / sign-in to the waitlist:
CREATE POLICY "Allow public insert into waitlist" 
ON public.waitlist 
FOR INSERT 
WITH CHECK (true);

-- Allow reading count or personal registration
CREATE POLICY "Allow anon read waitlist" 
ON public.waitlist 
FOR SELECT 
USING (true);
