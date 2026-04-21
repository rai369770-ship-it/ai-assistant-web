export interface Tool {
  id: string;
  name: string;
  description: string;
  category: ToolCategory;
}

export type ToolCategory = 
  | 'AI Tools'
  | 'Audio Tools'
  | 'Productivity Tools'
  | 'Video Tools'
  | 'Image Tools'
  | 'Device Tools';

export interface CategoryGroup {
  name: ToolCategory;
  tools: Tool[];
}
