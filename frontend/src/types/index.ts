export type MilestoneCategory = 'CAREER' | 'TRAVEL' | 'HEALTH' | 'RELATIONSHIP' | 'ACHIEVEMENT' | 'OTHER';
export type OccasionType = 'BIRTHDAY' | 'TET' | 'ANNIVERSARY' | 'CUSTOM';
export type WishLanguage = 'VI' | 'EN';

export interface User {
  id: string;
  email: string;
  displayName: string;
  createdAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  email: string;
  displayName: string;
}

export interface Recipient {
  id: string;
  name: string;
  birthday?: string;
  relationship?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Milestone {
  id: string;
  recipientId: string;
  description: string;
  category: MilestoneCategory;
  occurredAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface GeneratedWish {
  id: string;
  recipientId: string;
  recipientName: string;
  occasionType: OccasionType;
  language: WishLanguage;
  generatedText: string;
  editedText?: string;
  version: number;
  milestoneIds: string[];
  createdAt: string;
  updatedAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface ApiErrorResponse {
  success: boolean;
  status: number;
  error: string;
  message: string;
  validationErrors?: Record<string, string>;
  timestamp: string;
}
