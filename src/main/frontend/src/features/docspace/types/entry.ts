export interface DocSpaceItem {
  id: string | number;
  title: string;
  fileExst?: string;
  isFolder?: boolean;
  viewUrl?: string;
  requestTokens?: Array<{ requestToken?: string }>;
}
