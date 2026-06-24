INSERT INTO categories (id, name, slug)
VALUES
    ('44444444-4444-4444-4444-444444444444', 'Concerts', 'concerts'),
    ('55555555-5555-5555-5555-555555555555', 'Conferences', 'conferences'),
    ('66666666-6666-6666-6666-666666666666', 'Workshops', 'workshops'),
    ('77777777-7777-7777-7777-777777777777', 'Meetups', 'meetups'),
    ('88888888-8888-8888-8888-888888888888', 'Sports', 'sports')
ON CONFLICT (slug) DO NOTHING;
