import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { bookmarksService } from 'src/services/bookmarks.service'
import type { Threat } from 'src/types/threat'

export const useBookmarksStore = defineStore('bookmarks', () => {
  const bookmarks = ref<Threat[]>([])
  const bookmarkedIds = ref<Set<string>>(new Set())
  const isLoading = ref(false)
  const initialized = ref(false)

  const isBookmarked = computed(() => (id: string) => bookmarkedIds.value.has(id))

  async function load() {
    if (initialized.value) return
    isLoading.value = true
    try {
      bookmarks.value = await bookmarksService.getBookmarks()
      bookmarkedIds.value = new Set(bookmarks.value.map(t => t.id))
      initialized.value = true
    } finally {
      isLoading.value = false
    }
  }

  async function toggle(threat: Threat) {
    if (bookmarkedIds.value.has(threat.id)) {
      await bookmarksService.removeBookmark(threat.id)
      bookmarkedIds.value.delete(threat.id)
      bookmarks.value = bookmarks.value.filter(t => t.id !== threat.id)
    } else {
      await bookmarksService.addBookmark(threat.id)
      bookmarkedIds.value.add(threat.id)
      bookmarks.value.push(threat)
    }
  }

  return { bookmarks, bookmarkedIds, isBookmarked, isLoading, load, toggle }
})