-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/mgdiksdtwgiegoqnpbzy/sql

CREATE TABLE IF NOT EXISTS public.waitlist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    full_name TEXT,
    avatar_url TEXT,
    provider TEXT DEFAULT 'email',
    platform TEXT DEFAULT 'both',
    preferred_plan TEXT,
    preferred_price TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- If table already exists, add columns safely:
ALTER TABLE public.waitlist ADD COLUMN IF NOT EXISTS platform TEXT DEFAULT 'both';
ALTER TABLE public.waitlist ADD COLUMN IF NOT EXISTS preferred_plan TEXT;
ALTER TABLE public.waitlist ADD COLUMN IF NOT EXISTS preferred_price TEXT;

-- Enable Row Level Security (RLS)
ALTER TABLE public.waitlist ENABLE ROW LEVEL SECURITY;

-- Allow anyone (public anon) to insert their waitlist survey:
CREATE POLICY "Allow public insert into waitlist" 
ON public.waitlist 
FOR INSERT 
WITH CHECK (true);

-- Allow reading waitlist
CREATE POLICY "Allow anon read waitlist" 
ON public.waitlist 
FOR SELECT 
USING (true);
