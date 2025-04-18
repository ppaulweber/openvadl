#!/usr/bin/env python3

# Installs all hooks defined in hooks/ by creating symlinks it the
# local .git/hooks directory. This will enforce git to call the
# hooks e.g. on committing
# If a hook in hooks/ hasn't executable permissions, it will
# change the permissions by allowing u+x

import subprocess
import sys
import os
import stat


# finds the root directory of the git repository in the given directory
def find_git_root(from_path):
  try:
    # Run the git command to find the top-level git directory from the script's location
    result = subprocess.run(['git', 'rev-parse', '--show-toplevel'], check=True, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, text=True, cwd=from_path)
    git_root = result.stdout.strip()
    print(f"Git repository root: {git_root}")
    return git_root
  except subprocess.CalledProcessError as e:
    print("❌ Error finding git repository root. Are you inside a git repository?\n", e)
    sys.exit(1)


# Installs a given hook to the target location
def install_git_hook(hook_name, source_path, target_path):
  print(f"- {hook_name}: ", end="")

  if sys.platform == 'win32':
    # Symbolic links are weird (and require admin rights) under windows.
    # Instead we use a stub file that runs the actual hook.
    # git uses gitbash as /usr/bin/sh under windows. Thus, we create a bash
    # script that launches python with the actual hook, forwarding its
    # arguments with $@.
    target_content = f"#!/usr/bin/sh\npython '{source_path}' $@"
    if os.path.exists(target_path):
      if os.path.isfile(target_path):
        with open(target_path, 'r') as existing:
          if existing.read() == target_content:
            print("☑️ Skipping: already installed")
            return

      print(f"❌  Existing hook conflicts with the intended installation.\n  Hint: Remove {target_path} and try again.")
      return
    else:
      with open(target_path, 'w') as target_file:
        target_file.write(target_content)
        print(f"✅  Indirection file successfully created")
  else:
    if os.path.exists(target_path):
      # Determine if it's a symlink and points to the correct source
      if os.path.islink(target_path) and os.readlink(target_path) == source_path:
        print("☑️ Skipping: already installed")
      else:
        print(f"❌  Existing hook conflicts with the intended installation.\n  Hint: Remove {target_path} and try again.")
      return

    try:
      # Make the source executable if it's not already
      if not os.access(source_path, os.X_OK):
        os.chmod(source_path, os.stat(source_path).st_mode | stat.S_IXUSR)
        chmod_notice = "(updated to executable)"
      else:
        chmod_notice = "(already executable)"

      os.symlink(source_path, target_path)
      print(f"✅  Symlink successfully installed {chmod_notice}")
    except OSError as e:
      print(f"❌  Failed to create symlink: {e}")


# installs all hooks of given repo
def install_git_hooks(repo_path):
  source_hooks_dir = os.path.join(repo_path, 'config', 'git', 'hooks')
  target_hooks_dir = os.path.join(repo_path, '.git', 'hooks')

  # Ensure the source and target directories exist
  if not os.path.isdir(source_hooks_dir):
    print(f"Source hooks directory does not exist: {source_hooks_dir}")
    sys.exit(1)
  if not os.path.isdir(target_hooks_dir):
    print(f"Target hooks directory does not exist: {target_hooks_dir}")
    sys.exit(1)

  # List all files in the source hooks directory
  for hook_name in os.listdir(source_hooks_dir):
    source_hook_path = os.path.join(source_hooks_dir, hook_name)
    target_hook_path = os.path.join(target_hooks_dir, hook_name)

    install_git_hook(hook_name, source_hook_path, target_hook_path)


def check_python3_availability():
  try:
    # Since python3 is just python under windows and usr/bin/env does not exist,
    # we need to handle it a bit differently. Just specifying 'python' should
    # work, as python.exe's directory should be on the PATH when using a
    # 'typical' installation.
    args = ['python', '--version'] if sys.platform == 'win32' else ['/usr/bin/env', 'python3', '--version']
    # Try to run '/usr/bin/env python3 --version' to get the Python version
    result = subprocess.run(args, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    # If the command was successful, print the version and return True
    print(f"Python3 is available: {result.stdout.strip()}")
    return True
  except subprocess.CalledProcessError:
    # If the command failed, Python3 is not available
    print("Python3 is not available.")
    return False
  except FileNotFoundError:
    if sys.platform == 'win32':
      print("'python' not found.\n"
          + "Install it or add it to your PATH if it is already installed.\n"
          + "If you have just added it to your path, try restarting your shell"
          + "so changes can take effect.")
    else:
      # Under *nix, if '/usr/bin/env' is not found, it indicates an environmental
      # issue.
      print("'/usr/bin/env' command not found.")
    return False


if __name__ == "__main__":
  # Check if python3 is available
  if not check_python3_availability():
    sys.exit(1)
  # Get the directory where the script is located
  script_directory = os.path.dirname(os.path.abspath(__file__))
  git_root_path = find_git_root(script_directory)
  print("\nInstalling git hooks:")
  install_git_hooks(git_root_path)
