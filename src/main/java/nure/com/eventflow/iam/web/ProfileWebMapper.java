package nure.com.eventflow.iam.web;

import nure.com.eventflow.iam.application.CurrentUserView;
import nure.com.eventflow.iam.application.RegisterUserCommand;
import nure.com.eventflow.iam.application.UpdateProfileCommand;
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
