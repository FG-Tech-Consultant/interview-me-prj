# Feature 011: Dashboard Revamp & Navigation

## Overview

Replace the placeholder dashboard with a proper application shell featuring sidebar navigation and a rich dashboard with profile completeness, quick actions, recent activity, and key metrics.

## Problem Statement

After registering, users land on a bare placeholder dashboard showing only email/tenantId/userId and a "placeholder" message. There's no navigation to other features (profile, skills, billing, etc.) - users have to know the URLs. The app lacks a proper layout shell.

## Feature Summary

1. **App Layout Shell** - Sidebar navigation + top bar wrapping all authenticated pages
2. **Rich Dashboard** - Profile completeness tracker, quick actions, activity feed, metrics cards
3. **Navigation** - Sidebar with links to all features, user menu with logout

## Functional Requirements

### REQ-001: App Layout Shell
- Persistent sidebar navigation on all authenticated pages
- Sidebar items: Dashboard, Profile, Skills, Exports, LinkedIn Analyzer, Billing
- Collapsible sidebar (icon-only mode)
- Top bar with app name/logo and user menu (email + logout)
- Active route highlighting in sidebar
- Mobile-responsive (drawer on small screens)

### REQ-002: Dashboard - Profile Completeness
- Visual progress indicator (circular or linear) showing profile completion %
- Checklist of incomplete items: profile info, job experiences, education, skills, stories, slug
- Each item links to the relevant page/section

### REQ-003: Dashboard - Quick Actions
- Card grid with primary actions: Edit Profile, Add Skill, Export Resume, Analyze LinkedIn, View Public Profile
- Each card has icon, title, description, and links to the relevant page

### REQ-004: Dashboard - Statistics Cards
- Total skills count
- Total job experiences count
- Total stories count
- Coin wallet balance
- Profile views (if available) or chat sessions count
- Export count

### REQ-005: Dashboard - Recent Activity
- Recent exports with status
- Recent LinkedIn analyses with scores
- Recent chat sessions

### REQ-006: Dashboard - Public Profile Link
- Show public profile URL if slug is set
- Copy-to-clipboard button
- Link to slug settings if not set

## Out of Scope
- Notifications system
- Real-time updates (websockets)
- Dashboard customization/widgets reordering

## Technical Notes
- Use MUI Drawer for sidebar
- Use existing hooks/APIs - no new backend endpoints needed
- All data already available via existing API endpoints
- Layout component wraps all ProtectedRoute children
