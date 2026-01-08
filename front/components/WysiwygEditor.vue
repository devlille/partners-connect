<template>
  <div class="wysiwyg-editor">
    <!-- Toolbar -->
    <div class="wysiwyg-toolbar">
      <button
        type="button"
        class="wysiwyg-btn"
        :class="{ 'is-active': editor?.isActive('bold') }"
        :disabled="disabled"
        @click="editor?.chain().focus().toggleBold().run()"
        title="Gras"
      >
        <span class="wysiwyg-btn-icon">B</span>
      </button>
      <button
        type="button"
        class="wysiwyg-btn"
        :class="{ 'is-active': editor?.isActive('italic') }"
        :disabled="disabled"
        @click="editor?.chain().focus().toggleItalic().run()"
        title="Italique"
      >
        <span class="wysiwyg-btn-icon italic">I</span>
      </button>
      <button
        type="button"
        class="wysiwyg-btn"
        :class="{ 'is-active': editor?.isActive('underline') }"
        :disabled="disabled"
        @click="editor?.chain().focus().toggleUnderline().run()"
        title="Souligné"
      >
        <span class="wysiwyg-btn-icon underline">U</span>
      </button>
      <button
        type="button"
        class="wysiwyg-btn"
        :class="{ 'is-active': editor?.isActive('strike') }"
        :disabled="disabled"
        @click="editor?.chain().focus().toggleStrike().run()"
        title="Barré"
      >
        <span class="wysiwyg-btn-icon line-through">S</span>
      </button>

      <span class="wysiwyg-separator" />

      <button
        type="button"
        class="wysiwyg-btn"
        :class="{ 'is-active': editor?.isActive('heading', { level: 2 }) }"
        :disabled="disabled"
        @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()"
        title="Titre"
      >
        <span class="wysiwyg-btn-icon">H</span>
      </button>
      <button
        type="button"
        class="wysiwyg-btn"
        :class="{ 'is-active': editor?.isActive('bulletList') }"
        :disabled="disabled"
        @click="editor?.chain().focus().toggleBulletList().run()"
        title="Liste à puces"
      >
        <UIcon name="i-heroicons-list-bullet" class="wysiwyg-icon" />
      </button>
      <button
        type="button"
        class="wysiwyg-btn"
        :class="{ 'is-active': editor?.isActive('orderedList') }"
        :disabled="disabled"
        @click="editor?.chain().focus().toggleOrderedList().run()"
        title="Liste numérotée"
      >
        <UIcon name="i-heroicons-numbered-list" class="wysiwyg-icon" />
      </button>

      <span class="wysiwyg-separator" />

      <button
        type="button"
        class="wysiwyg-btn"
        :class="{ 'is-active': editor?.isActive('link') }"
        :disabled="disabled"
        @click="setLink"
        title="Lien"
      >
        <UIcon name="i-heroicons-link" class="wysiwyg-icon" />
      </button>
      <button
        type="button"
        class="wysiwyg-btn"
        :disabled="disabled || !editor?.isActive('link')"
        @click="editor?.chain().focus().unsetLink().run()"
        title="Supprimer le lien"
      >
        <UIcon name="i-heroicons-link-slash" class="wysiwyg-icon" />
      </button>

      <span class="wysiwyg-separator" />

      <button
        type="button"
        class="wysiwyg-btn"
        :disabled="disabled"
        @click="editor?.chain().focus().undo().run()"
        title="Annuler"
      >
        <UIcon name="i-heroicons-arrow-uturn-left" class="wysiwyg-icon" />
      </button>
      <button
        type="button"
        class="wysiwyg-btn"
        :disabled="disabled"
        @click="editor?.chain().focus().redo().run()"
        title="Rétablir"
      >
        <UIcon name="i-heroicons-arrow-uturn-right" class="wysiwyg-icon" />
      </button>
    </div>

    <!-- Editor Content -->
    <EditorContent :editor="editor" class="wysiwyg-content" />
  </div>
</template>

<script setup lang="ts">
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Link from '@tiptap/extension-link'
import Underline from '@tiptap/extension-underline'

interface Props {
  modelValue: string
  placeholder?: string
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '',
  disabled: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit,
    Underline,
    Link.configure({
      openOnClick: false,
      HTMLAttributes: {
        class: 'text-primary-600 underline'
      }
    })
  ],
  editable: !props.disabled,
  onUpdate: ({ editor }) => {
    emit('update:modelValue', editor.getHTML())
  }
})

watch(() => props.modelValue, (value) => {
  if (editor.value && editor.value.getHTML() !== value) {
    editor.value.commands.setContent(value, false)
  }
})

watch(() => props.disabled, (disabled) => {
  editor.value?.setEditable(!disabled)
})

function setLink() {
  const previousUrl = editor.value?.getAttributes('link').href
  const url = window.prompt('URL du lien:', previousUrl)

  if (url === null) {
    return
  }

  if (url === '') {
    editor.value?.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }

  editor.value?.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
}

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped>
.wysiwyg-editor {
  border: 1px solid #d1d5db;
  border-radius: 0.5rem;
  overflow: hidden;
}

.wysiwyg-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  padding: 0.5rem;
  background-color: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.wysiwyg-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  padding: 0.25rem;
  border: none;
  border-radius: 0.25rem;
  background-color: transparent;
  color: #374151;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.wysiwyg-btn:hover:not(:disabled) {
  background-color: #e5e7eb;
}

.wysiwyg-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.wysiwyg-btn.is-active {
  background-color: #dbeafe;
  color: #1d4ed8;
}

.wysiwyg-btn-icon {
  font-weight: 600;
  font-size: 0.875rem;
}

.wysiwyg-btn-icon.italic {
  font-style: italic;
}

.wysiwyg-btn-icon.underline {
  text-decoration: underline;
}

.wysiwyg-btn-icon.line-through {
  text-decoration: line-through;
}

.wysiwyg-icon {
  width: 1rem;
  height: 1rem;
}

.wysiwyg-separator {
  width: 1px;
  height: 1.5rem;
  margin: 0 0.25rem;
  background-color: #d1d5db;
  align-self: center;
}

.wysiwyg-content {
  min-height: 12rem;
  padding: 0.75rem;
  background-color: white;
}

.wysiwyg-content :deep(.tiptap) {
  outline: none;
  min-height: 10rem;
}

.wysiwyg-content :deep(.tiptap p) {
  margin: 0 0 0.5rem 0;
}

.wysiwyg-content :deep(.tiptap h2) {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0 0 0.5rem 0;
}

.wysiwyg-content :deep(.tiptap ul),
.wysiwyg-content :deep(.tiptap ol) {
  padding-left: 1.5rem;
  margin: 0 0 0.5rem 0;
}

.wysiwyg-content :deep(.tiptap ul) {
  list-style-type: disc;
}

.wysiwyg-content :deep(.tiptap ol) {
  list-style-type: decimal;
}

.wysiwyg-content :deep(.tiptap a) {
  color: #2563eb;
  text-decoration: underline;
}

.wysiwyg-content :deep(.tiptap p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: #9ca3af;
  pointer-events: none;
  height: 0;
}
</style>
