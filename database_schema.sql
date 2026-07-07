-- ====================================================================
--              مخطط قاعدة بيانات منصة "سوق" للخدمات المحلية والذكاء الاصطناعي
--                          Supabase PostgreSQL Database Schema
-- ====================================================================

-- تفعيل ملحقات قاعدة البيانات المطلوبة (Extensions)
create extension if not exists "uuid-ossp";

-- 1. جدول المستخدمين (Users Table)
create table if not exists public.users (
    id uuid references auth.users on delete cascade primary key,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    phone varchar(20) unique,
    name text not null,
    email text unique,
    avatar_url text,
    city text default 'الرياض'::text,
    neighborhood text,
    role varchar(20) default 'CLIENT'::text check (role in ('CLIENT', 'TECH', 'ADMIN')),
    latitude double precision,
    longitude double precision,
    is_active boolean default true
);

-- 2. جدول الفنيين ومقدمي الخدمات (Technicians Table)
create table if not exists public.technicians (
    id uuid default gen_random_uuid() primary key,
    user_id uuid references public.users(id) on delete cascade not null unique,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    profession text not null, -- التخصص: سباكة، كهرباء، تكييف، دهان...الخ
    experience integer default 0, -- سنوات الخبرة
    bio text, -- نبذة عن الفني
    previous_works_urls text[], -- معرض الأعمال (مصفوفة روابط الصور والفيديوهات)
    districts text[], -- الأحياء التي يغطيها
    is_available boolean default true, -- حالة التوفر (متاح / غير متاح)
    rating numeric(3, 2) default 5.00 check (rating >= 1.0 and rating <= 5.0), -- تقييم الفني
    completed_orders integer default 0 -- عدد الطلبات المنجزة
);

-- 3. جدول التصنيفات (Categories Table)
create table if not exists public.categories (
    id uuid default gen_random_uuid() primary key,
    name text not null unique,
    icon text, -- رمز التصنيف (مثال: fa-faucet)
    bg_color text -- لون الخلفية المناسب للثيم المعتمد
);

-- 4. جدول طلبات الصيانة والوظائف من العملاء (Requests / Jobs Table)
create table if not exists public.requests (
    id uuid default gen_random_uuid() primary key,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    client_id uuid references public.users(id) on delete cascade not null,
    service_type text not null, -- نوع الخدمة المطلوبة
    description text not null, -- وصف المشكلة بدقة
    image_urls text[], -- صور المشكلة المرفوعة
    video_url text, -- فيديو اختياري للمشكلة
    location text, -- الموقع الجغرافي أو الوصف النصي
    neighborhood text, -- الحي
    urgent_level varchar(20) default 'NORMAL' check (urgent_level in ('NORMAL', 'URGENT', 'CRITICAL')),
    status varchar(30) default 'NEW' check (status in ('NEW', 'APPROVED', 'IN_TRANSIT', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    tech_id uuid references public.users(id) on delete set null, -- الفني الذي قبل الطلب
    budget text, -- الميزانية التقديرية للوظيفة
    rating_stars integer check (rating_stars >= 1 and rating_stars <= 5),
    rating_comment text
);

-- 5. جدول عروض الفنيين الممولة والترقيات (Offers Table)
create table if not exists public.offers (
    id uuid default gen_random_uuid() primary key,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    tech_id uuid references public.users(id) on delete cascade not null,
    title text not null, -- عنوان العرض (مثال: غسيل 3 مكيفات بسعر مميز)
    description text not null, -- تفاصيل العرض
    price text not null, -- السعر الإجمالي
    image_url text, -- صورة توضيحية للعرض
    is_sponsored boolean default false, -- هل العرض ممول / مثبت كإعلان في القائمة الجانبية؟
    sponsored_tier varchar(20) default 'FREE' check (sponsored_tier in ('FREE', 'GOLD', 'PLATINUM'))
);

-- 6. جدول القنوات (Channels Table)
create table if not exists public.channels (
    id uuid default gen_random_uuid() primary key,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    name text not null unique, -- اسم القناة (مثال: حكايات الياسمين، عروض الكهرباء)
    description text, -- نبذة عن القناة
    logo_url text, -- لوغو القناة
    creator_id uuid references public.users(id) on delete cascade not null,
    is_verified boolean default false
);

-- 7. جدول منشورات القنوات والحي (Posts Table)
create table if not exists public.posts (
    id uuid default gen_random_uuid() primary key,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    channel_id uuid references public.channels(id) on delete cascade, -- القناة المنتمي لها المنشور (اختياري، إن وجد)
    user_id uuid references public.users(id) on delete cascade not null, -- ناشر المنشور
    tag text default 'عام'::text, -- وسم: سؤال فني، إعلان خدمة، نصائح، خصومات
    content text not null, -- نص المنشور
    likes_count integer default 0,
    liked_by uuid[] default array[]::uuid[], -- قائمة بـ uuids للمستخدمين الذين تفاعلوا بالإعجاب
    image_urls text[], -- صور المنشور
    video_urls text[] -- مقاطع الفيديو المرفقة بالمنشور
);

-- 8. جدول غرف المحادثات (Chats Table)
create table if not exists public.chats (
    id uuid default gen_random_uuid() primary key,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    client_id uuid references public.users(id) on delete cascade not null,
    tech_id uuid references public.users(id) on delete cascade not null,
    last_message text,
    last_message_time timestamp with time zone default timezone('utc'::text, now()),
    unique (client_id, tech_id)
);

-- 9. جدول رسائل المحادثات المباشرة (Messages Table)
create table if not exists public.messages (
    id uuid default gen_random_uuid() primary key,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    chat_id uuid references public.chats(id) on delete cascade not null,
    sender_id uuid references public.users(id) on delete cascade not null,
    text text, -- نص الرسالة
    image_url text, -- صورة مشاركة داخل الشات
    audio_url text, -- تسجيل صوتي صوت / ريكورد
    location_lat double precision, -- إحداثيات مشاركة الموقع
    location_lng double precision,
    is_read boolean default false
);

-- 10. جدول الإشعارات (Notifications Table)
create table if not exists public.notifications (
    id uuid default gen_random_uuid() primary key,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    user_id uuid references public.users(id) on delete cascade not null,
    title text not null, -- عنوان الإشعار (طلب جديد، قبول الطلب، رسالة جديدة)
    message text not null, -- محتوى الإشعار بالتفصيل
    is_read boolean default false,
    type varchar(50) -- نوع الإشعار: order_update, message, advertisement, rating
);

-- 11. جدول بلاغات المستخدمين والمحتوى (Reports Table)
create table if not exists public.reports (
    id uuid default gen_random_uuid() primary key,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    reporter_id uuid references public.users(id) on delete cascade not null,
    reported_id uuid references public.users(id) on delete cascade,
    reason text not null, -- سبب البلاغ
    target_type varchar(50) not null, -- نوع البلاغ: مستخدم، فني، منشور، إعلان
    target_id uuid not null
);

-- 12. جدول الإعدادات العامة (Settings Table)
create table if not exists public.settings (
    id uuid default gen_random_uuid() primary key,
    key text unique not null,
    value text not null
);

-- ====================================================================
--                    إنشاء الفهارس لتحسين سرعة وأداء الاستعلامات
-- ====================================================================
create index if not exists idx_users_neighborhood on public.users(neighborhood, city);
create index if not exists idx_tech_profession on public.technicians(profession, is_available);
create index if not exists idx_requests_status on public.requests(status, client_id, tech_id);
create index if not exists idx_offers_sponsored on public.offers(is_sponsored);
create index if not exists idx_posts_channel on public.posts(channel_id, created_at desc);
create index if not exists idx_messages_chat on public.messages(chat_id, created_at asc);
create index if not exists idx_notifications_user on public.notifications(user_id, is_read);

-- ====================================================================
--                  تفعيل نظام حماية قاعدة البيانات (Row Level Security)
-- ====================================================================
alter table public.users enable row level security;
alter table public.technicians enable row level security;
alter table public.categories enable row level security;
alter table public.requests enable row level security;
alter table public.offers enable row level security;
alter table public.channels enable row level security;
alter table public.posts enable row level security;
alter table public.chats enable row level security;
alter table public.messages enable row level security;
alter table public.notifications enable row level security;

-- أمثلة لسياسات الحماية العامة (RLS Policies):
-- السماح للمستخدم بقراءة ملفه الشخصي وتعديله
create policy "Users can view all profiles" on public.users for select using (true);
create policy "Users can update their own profile" on public.users for update using (auth.uid() = id);

-- السماح للجميع بمشاهدة الفنيين
create policy "Anyone can view technicians" on public.technicians for select using (true);

-- السماح للجميع بمشاهدة المنشورات والقنوات والعروض الممولة
create policy "Anyone can view categories" on public.categories for select using (true);
create policy "Anyone can view offers" on public.offers for select using (true);
create policy "Anyone can view channels" on public.channels for select using (true);
create policy "Anyone can view posts" on public.posts for select using (true);

-- إدراج تصنيفات أولية للتجربة (Initial Seed Data)
insert into public.categories (name, icon, bg_color) values
('سباكة وصيانة 🚰', 'fa-faucet text-blue-600', 'bg-blue-50'),
('كهرباء وإنارة ⚡', 'fa-bolt text-amber-500', 'bg-amber-50'),
('تكييف وتبريد ❄️', 'fa-snowflake text-sky-500', 'bg-sky-50'),
('طلاء ودهانات 🎨', 'fa-palette text-purple-500', 'bg-purple-50'),
('تنظيف شامل 🧹', 'fa-broom text-teal-500', 'bg-teal-50'),
('أعمال حدادة 🧱', 'fa-hammer text-slate-500', 'bg-slate-50')
on conflict (name) do nothing;
