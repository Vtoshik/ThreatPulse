import axiosInstance from './api'
import { mapThreat } from './threats.service'
import type { ApiThreat, Threat } from 'src/types/threat'

export const bookmarksService = {
  async getBookmarks(): Promise<Threat[]> {
    const response = await axiosInstance.get<ApiThreat[]>('/api/bookmarks')
    return response.data.map(mapThreat)
  },

  async getBookmarkedIds(): Promise<Set<string>> {
    const response = await axiosInstance.get<number[]>('/api/bookmarks/ids')
    return new Set(response.data.map(String))
  },

  async addBookmark(threatId: string | number): Promise<void> {
    await axiosInstance.post(`/api/bookmarks/${threatId}`)
  },

  async removeBookmark(threatId: string | number): Promise<void> {
    await axiosInstance.delete(`/api/bookmarks/${threatId}`)
  },
}