/*
 * Copyright (c) 2018, Google and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "jvmti.h"

#include <pthread.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifndef JNI_ENV_ARG

#ifdef __cplusplus
#define JNI_ENV_ARG(x, y) y
#define JNI_ENV_PTR(x) x
#else
#define JNI_ENV_ARG(x,y) x, y
#define JNI_ENV_PTR(x) (*x)
#endif

#endif

static char *filename = NULL;
static char default_file[] = "stackTraces.csv";
static int max_frame_count = 8;
static int sampling = 512 * 1024;

#define BUFFER_LEN 10 * 1024 * 1024
static char buffer[BUFFER_LEN];
static int buffer_pos = 0;
pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

static jvmtiEnv *jvmti = NULL;

// Start of the JVMTI agent code.
static int check_error(jvmtiError err, const char *s) {
  if (err != JVMTI_ERROR_NONE) {
    printf("  ## %s error: %d\n", s, err);
    return 1;
  }
  return 0;
}

static jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved);

JNIEXPORT
jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT
jint JNICALL Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT
jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
  return JNI_VERSION_1_8;
}



// Given a method and a location, this method gets the line number.
static
jint get_line_number(jvmtiEnv* jvmti, jmethodID method,
                     jlocation location) {
  // Read the line number table.
  jvmtiLineNumberEntry *table_ptr = 0;
  jint line_number_table_entries;
  int l;
  jlocation last_location;
  int jvmti_error = (*jvmti)->GetLineNumberTable(jvmti, method,
                                                 &line_number_table_entries,
                                                 &table_ptr);

  if (JVMTI_ERROR_NONE != jvmti_error) {
    return -1;
  }
  if (line_number_table_entries <= 0) {
    return -1;
  }
  if (line_number_table_entries == 1) {
    return table_ptr[0].line_number;
  }

  // Go through all the line numbers...
  last_location = table_ptr[0].start_location;
  for (l = 1; l < line_number_table_entries; l++) {
    // ... and if you see one that is in the right place for your
    // location, you've found the line number!
    if ((location < table_ptr[l].start_location) &&
        (location >= last_location)) {
      return table_ptr[l - 1].line_number;
    }
    last_location = table_ptr[l].start_location;
  }

  if (location >= last_location) {
    return table_ptr[line_number_table_entries - 1].line_number;
  } else {
    return -1;
  }
}

void write_buffer() {
  FILE *fptr = fopen(filename, "a+");
  fprintf(fptr, "%s", buffer);
  fclose(fptr);
  memset(buffer, 0, sizeof(buffer));
  buffer_pos = 0;
}

JNIEXPORT
void JNICALL Agent_OnUnload(JavaVM *jvm) {
  pthread_mutex_lock(&mutex);
  write_buffer();
  pthread_mutex_unlock(&mutex);
}

static void write_object(jvmtiEnv *jvmti_env,
                  JNIEnv* jni_env,
                  jthread thread,
                  jobject object,
                  jclass object_klass,
                  jlong size) {

  long obj_id = (long) (void*)((long*)object)[0];

  jvmtiFrameInfo frames[max_frame_count];
  jint frame_count;
  jvmtiError err;

  err = (*jvmti)->GetStackTrace(jvmti, thread, 0, max_frame_count, frames, &frame_count);
  if (err == JVMTI_ERROR_NONE && frame_count >= 1) {
    char local_buffer[max_frame_count * 2048];
    int local_buffer_len = 0;

    size_t i;
    for (i = 0; i < frame_count; i++) {
      jlocation bci = frames[i].location;
      jmethodID methodid = frames[i].method;
      char *name = NULL, *signature = NULL, *class_name = NULL;
      jclass declaring_class;

      int line_number = get_line_number(jvmti, methodid, bci);

      (*jvmti)->GetMethodName(jvmti, methodid, &name, &signature, 0);
      (*jvmti)->GetMethodDeclaringClass(jvmti, methodid, &declaring_class);
      (*jvmti)->GetClassSignature(jvmti, declaring_class, &class_name, NULL);

      local_buffer_len += sprintf(local_buffer + local_buffer_len, "%lu\t%ld\t%s\t%s\t%d\t%s\n", obj_id,i,name,signature,line_number,class_name);
    }
    pthread_mutex_lock(&mutex);
    if (buffer_pos + strlen(local_buffer) < BUFFER_LEN) {
      buffer_pos += sprintf(buffer+buffer_pos, "%s", local_buffer);
    } else {
      write_buffer();
      buffer_pos += sprintf(buffer+buffer_pos, "%s", local_buffer);
    }
    pthread_mutex_unlock(&mutex);
  }
}

JNIEXPORT
void JNICALL SampledObjectAlloc(jvmtiEnv *jvmti_env,
                                JNIEnv* jni_env,
                                jthread thread,
                                jobject object,
                                jclass object_klass,
                                jlong size) {
  write_object(jvmti_env, jni_env, thread, object, object_klass, size);
}

JNIEXPORT
void JNICALL VMObjectAlloc(jvmtiEnv *jvmti_env,
                           JNIEnv* jni_env,
                           jthread thread,
                           jobject object,
                           jclass object_klass,
                           jlong size) {
  write_object(jvmti_env, jni_env, thread, object, object_klass, size);
}

static int enable_notifications() {
  if (check_error((*jvmti)->SetEventNotificationMode(
      jvmti, JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, NULL),
                     "Set event notifications")) {
    return 1;
  }

  return check_error((*jvmti)->SetEventNotificationMode(
      jvmti, JVMTI_ENABLE, JVMTI_EVENT_SAMPLED_OBJECT_ALLOC, NULL),
                     "Set event notifications");
}

static void set_sampling_interval(int value) {
  (*jvmti)->SetHeapSamplingInterval(jvmti, value);
}

static int parse_sampling(char *value) {
  int i = strlen(value) - 1;
  if (value[i] == 'b' || value[i] == 'B') {
      if (value[i-1] > '9') {
          i--;
      }
  }

  int mult = 1;
  if (value[i] == 'k' || value[i] == 'K') {
    mult = 1024;
  } else if (value[i] == 'm' || value[i] == 'M') {
    mult = 1024 * 1024;
  } else if (value[i] == 'g' || value[i] == 'G') {
    mult = 1024 * 1024 * 1024;
  }

  char *s = malloc((i+1) * sizeof(char));
  strncpy(s, value, i);
  s[i] = '\0';
  int samp = atoi(s);
  free(s);

  return samp * mult;
}

static void check_option(char *option, char *value) {
    if (strcmp(option, "depth") == 0) {
        max_frame_count = atoi(value);
    } else if (strcmp(option, "file") == 0) {
        filename = malloc((strlen(value)+1) * sizeof(char));
        strcpy(filename, value);
    } else if (strcmp(option, "sampling") == 0) {
        sampling = parse_sampling(value);
    }
}

static void parse_arguments(char *args) {
  if (args != NULL) {
    char *arguments = malloc((strlen(args) + 1)* sizeof(char));
    strcpy(arguments, args);
    char arguments_delim[] = ",";
    char inside_delim[] = "=";

    char *save_ptr, *ptr = strtok_r(arguments, arguments_delim, &save_ptr);
    while (ptr != NULL)
    {
      char *arg = malloc((strlen(ptr) + 1) * sizeof(char));
      strcpy(arg, ptr);

      char *in_save_ptr, *opt = strtok_r(arg, inside_delim, &in_save_ptr);
      char *value = strtok_r(NULL, inside_delim, &in_save_ptr);
      check_option(opt, value);

      free(arg);

      ptr = strtok_r(NULL, arguments_delim, &save_ptr);
    }
    free(arguments);
  }

  if (filename == NULL) {
      filename = default_file;
  }
}

static
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
  parse_arguments(options);
  remove(filename);
  jint res;
  jvmtiEventCallbacks callbacks;
  jvmtiCapabilities caps;

  res = JNI_ENV_PTR(jvm)->GetEnv(JNI_ENV_ARG(jvm, (void **) &jvmti),
                                 JVMTI_VERSION_9);
  if (res != JNI_OK || jvmti == NULL) {
    fprintf(stderr, "Error: wrong result of a valid call to GetEnv!\n");
    return JNI_ERR;
  }

  memset(&callbacks, 0, sizeof(callbacks));
  callbacks.SampledObjectAlloc = &SampledObjectAlloc;
  callbacks.VMObjectAlloc = &VMObjectAlloc;

  memset(&caps, 0, sizeof(caps));
  // Get line numbers, sample events, filename, and gc events for the tests.
  caps.can_get_line_numbers = 1;
  caps.can_get_source_file_name = 1;
  caps.can_generate_garbage_collection_events = 1;
  caps.can_generate_sampled_object_alloc_events = 1;
  caps.can_generate_vm_object_alloc_events = 1;
  if (check_error((*jvmti)->AddCapabilities(jvmti, &caps), "Add capabilities")) {
    return JNI_ERR;
  }

  if (check_error((*jvmti)->SetEventCallbacks(jvmti, &callbacks,
                                              sizeof(jvmtiEventCallbacks)),
                  " Set Event Callbacks")) {
    return JNI_ERR;
  }
  //ADD HERE
  enable_notifications();
  set_sampling_interval(sampling);
  return JNI_OK;
}

#ifdef __cplusplus
}
#endif
