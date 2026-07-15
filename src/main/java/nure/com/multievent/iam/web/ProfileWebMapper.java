package nure.com.multievent.iam.web;

import nure.com.multievent.iam.application.ChangePasswordCommand;
import nure.com.multievent.iam.application.CurrentUserView;
import nure.com.multievent.iam.application.RegisterUserCommand;
import nure.com.multievent.iam.application.UpdateProfileCommand;
import org.springframework.stereotype.Component;

@Component
public class ProfileWebMapper {

    RegisterUserCommand toCommand(RegisterForm form) {
        return new RegisterUserCommand(
                form.getEmail(),
                form.getPassword(),
                form.getFirstName(),
                form.getLastName(),
                form.getPhoneNumber(),
                form.getCity(),
                form.getBio());
    }

    UpdateProfileCommand toCommand(ProfileEditForm form) {
        return new UpdateProfileCommand(
                form.getFirstName(),
                form.getLastName(),
                form.getPhoneNumber(),
                form.getCity(),
                form.getBio());
    }

    ChangePasswordCommand toCommand(ChangePasswordForm form) {
        return new ChangePasswordCommand(
                form.getCurrentPassword(),
                form.getNewPassword(),
                form.getConfirmPassword());
    }

    ProfileEditForm toEditForm(CurrentUserView currentUser) {
        ProfileEditForm form = new ProfileEditForm();
        form.setFirstName(currentUser.firstName());
        form.setLastName(currentUser.lastName());
        form.setPhoneNumber(currentUser.phoneNumber());
        form.setCity(currentUser.city());
        form.setBio(currentUser.bio());
        return form;
    }
}
