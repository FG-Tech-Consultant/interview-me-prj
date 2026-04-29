export interface NotificationItem {
  id: number;
  type: string;
  title: string;
  message: string;
  referenceType: string | null;
  referenceId: number | null;
  read: boolean;
  createdAt: string;
}
